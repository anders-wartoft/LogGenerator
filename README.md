# LogGenerator
N.B., the format for configuration has radically changed in version 1.1. Please see the Latest Release Notes section below.

LogGenerator is a tool to debug log streams, i.e., syslog, Kafka, UDP diodes and similar chains of log collection systems.
The tool reads input from an input module, filters the input (add a header, replace text etc.) and finally writes the output with an output module.

LogGenerator uses input modules, filters and output modules and combines that into a chain. Each event from the input module is processed by zero or more filters, that can rewrite the contents. After filtration the events are written with an output module.

Example: Read a log file, add a syslog header and write to a remote Kafka. In the syslog header, add a counter that starts at 100 and increases with each string. Also, add statistics messages (beginning of transaction etc.).
When the events are stored in Kafka, start another LogGenerator that fetches the Kafka events, checks the counter and writes the events to null (to increase performance). Give a measurement of the time elapsed, how many items were received, the event per second and an estimate of the bandwidth usage as well as a list of missed events (counter integers that are missing) and the next counter number we are expecting.

The example above is literally two commands. 

```bash
java -jar target/LogGenerator{version}.jar -i file --name src/test/data/test.txt -f header -st "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{counter:a:100}]: " -o kafka -t OUTPUT -b 192.168.153.129:9092 -ci test2
java -jar target/LogGenerator{version}.jar -i kafka -ci test3 -t OUTPUT -b 192.168.153.129:9092 -f gap -r "\[(\d+)\]:" -o cmd -s true 
```
When running the last command, press Ctrl-C to see the gaps in the received data. Since we started the counter on 100, there should at least be one gap: 1-99.

### Latest Release Notes
#### 1.1-4
Changed kafka-clients dependency version from 3.7.1 to 3.9.1 due to CVE-2025-27817

#### 1.1-3
Added a parameter {all} to regex filter, so a regex filter can wrap the log in a new string. Also, fixed a bug that hindered the regex filter to escape quotes (changing " to \").
Minor documentation updates.

#### 1.1-2
Update of documentation. E.g., -h is no longer valid as --hostname shorthand. Also, update of `-f guard`. In 1.1-1, the `-f guard` command removed all content in the event but not the event itself, so if the event was written to file, an empty line would be the result. In 1.1-2, the event is correctly removed.

New Input: StringInputItem. This works like Template but you don't need to create a file with the template, you can just add it on the command line.

#### 1.1-1
1.1-1 Updated kafka-clients dependency due to security vulnerability in earlier versions of the Kafka-client library used.

CVE-2024-31141 Moderate severity.

#### 1.1-SNAPSHOT
- Major refactoring of the configuration system. 
  The main method now only accepts the following parameters:
  `-h` or `--help` to get help
  `-i` or `--input` to specify the input module
  `-o` or `--output` to specify the output module
  `-f` or `--filter` to specify the filter module
  `-pf` or `--property-file` to specify a properties file
  `-vf` or `--variable-file` to specify a variable file
  `-l` or `--limit` to specify the number of events to send (0 means no limit)
  `-e` or `--eps` to specify the number of events per second to send (0 means no limit)
  `-s` or `--statistics` to add statistics messages and printouts
 
  After `-i {module}` you can add parameters for the input module. The parameters are module specific. 
  After `-o {module}` you can add parameters for the output module. The parameters are module specific.
  After `-f {module}` you can add parameters for the filter module. The parameters are module specific.
  To see the available parameters for a module, use `-h` or `--help` after the module name, e.g., `-i file -h`.

  The main reason for this change is to be able to add several input modules, filters and output modules of the same type in the same command line,
  e.g., `-i file --name file1.txt -i file --name file2.txt -o cmd -o file --name file3.txt -o file --name file4.txt`.

  Properties in a properties file must now be specified in order, since the order is now important.
- DateSubstitute now supports epoch16 format
- Headers, Regex and Templates now support different time offsets. This is useful when you want to send events with a timestamp that is not the current time.

### Input modules:
There are input module for the following tasks:
- Read files
- Read files in a directory
- Read files in a directory with globs
- Read JSON file
- Receive UDP
- Receive TCP
- Receive TCP SSL
- Fetch from Kafka topics
- Fetch from Elasticsearch
- Static string
- Static string ending with a counter starting from 1
- Dynamic string with variable substitution

#### Read files
Read a local file. (For Template files, see below)

Parameters: `-i file --name {file name}`

Example: `-i file --name ./src/test/data/test.txt`

The implementation actually wraps this in a directory input module, so if you print the configuration
with log4j2, you will see that the file input is actually a directory input.

#### Read files in a directory
Read all files in a directory

Parameters: `-i file --name {directory name}`

Example: `-i file --name ./src/test/data/`

#### Read files in a directory with globs
Read all files in a directory that matches a glob. See https://javapapers.com/java/glob-with-java-nio/ for details on how to write globs.

Parameters: `-i file --name {directory name -g "{glob}"`). 

Example: `-i file --name ./src/test/data/ -g "**.txt"`

Note that all * must be within quotes, since otherwise, the OS will expand that variable.

#### Read JSON file
If a file is in JSON format (not line json but the entire file is one JSON object) you can read the file with the JSON File input.

Parameters: `-i json-file --name {filename}`

Example: `-i json-file --name ./src/test/data/elasticsearch.json`

If the file contains an array you would like to extract, use the parameter `--path` `-p`.
E.g., the JSON output from an Elastic query is structured like this:
``` 
{
  "took": 1,
  "timed_out": false,
  ...
  "hits": {
    "total": {
      "value": 33,
      "relation": "eq"
    },
    "max_score": 1,
    "hits": [
      {
        "_index": "testindex",
        "_id": "test2-11",
        ...
      },
      {
        "_index": "testindex",
        "_id": "test2-22",
        ...
```
To read this response from file, use the `json-file` input and set `-p` to `hits.hits`. The result will be an array of elements and each element will be emitted as a new event.
So, to extract the `_id` from each element, add a `json` filter with `-p _id`. Now only the `_id` field will be propagated.

The command line will then become:
`java -jar LogGenerator-{version}.jar -i json-file --name ./src/test/data/elasticsearch.json -p hits.hits -f json -p _id -o cmd`

#### Receive UDP
Set up a UDP server. 

Parameters: `-i udp [--name {host}][ -p portnumber`

Example: `-i udp --hostname localhost --port 5999` or `-i udp -p 5999`

#### Receive TCP
Set up a TCP server. 

Parameters: `-i tcp [--name {host}] -ip portnumber`

Example: `-i tcp --hostname localhost --port 5999` or `-i tcp -p 5999`

If you try these examples, note that there is no output module specified, so the events will be discarded. Try adding `-o cmd` to see the events.

### Receive TCP SSL
Set up a TCP server with encrypted communication.

Parameters and example, see below in the Q&A section.

#### Fetch from Kafka topics
Connect to a Kafka server and read from a topic

Parameters: `-i kafka -ci {client id} -t {topic name} -b {boostrap server}`

Example: `-i kafka -ci test -t testtopic -b localhost:9092`

#### Fetch from Elasticsearch index
Connect to an Elasticsearch instance and read from an index:

The main use case is to get one field from an index and send to a receiver, for example a gap detector to be able to see if there are missing events (or duplicates) in the index.

Parameters: `-i elastic --hostname {hostname or ip} -p {port number} -i {index name} -f {field to get} -ak {API key} -c {x.509 certificate in cer format} -q {query string in query dsl format}`

Example:
``` properties
# Elasticsearch
input=elastic
# Elastic instance to connect to
hostname=192.168.0.40
# Port number
port=9200
# Index to read from
index=testindex
# The API key to use
api-key=QTNTTzM0b0ItZ0x3UEpJTTh1Z0k6cEF6Zk42NGhSdEcwUTFpYWE2Y0hBQQ==
# Field to use as id
field=_id
# cer file of the elastic server
certificate-path=./target/elastic.cer
# Query for the input item
query={"query": { "query_string": { "query": "*" }}, "_source": ["_id"]}
# How many items to fetch from Elastic in each REST request
batch-size=1000

# Detect gaps in the id:s. Since we extract only the _id field, this should suffice
filter=gap
regex=-(\d+)$
# Check if we have duplicate id:s in the stream
duplicate-detection=true

# Output
output=cmd
```

#### Static string
Send the same string over and over again. 

Parameters: `-i static --string {the string to send}`

Example: `-i static --string "Test string to send"`

Example: `-i static --string "Test string to send" -l 1000` (only send 1000 events)

#### Static string ending with a counter starting from 1
This is used to send a static string that is appended with an increasing number, starting from 1. This is a very fast way to send events ending with a counter.
To send 1.000.000 events from "Test:1" to "Test:1000000", use `java -jar LogGenerator-with-dependencies.jar -i counter -string "Test:" --limit 1000000 -o cmd`

Parameters: `-i counter --string {the string to send}`

Example: `-i counter --string "Test string number:"`

If you want to test the generation speed, use the `null` output since that is faster than writing to the console. Add `-s true` for measurements: `java -jar target/LogGenerator-{version}.jar -i counter --string "Test:" --limit 1000000 -o null -s true`

#### Dynamic string with variable substitution
This input item works approximately like the Template item, but you can specify a string from the command line instead of a file.

Parameters: `--from {the string to send, with variables} --template continuous --time-offset -10000`
```
--from, -fr - The string to use as a template
--template, -t - [continuous, once, time:{time in ms}]
--time-offset, -to - Time in ms to add or subtract from the current date, if used as a variable
```

Example: `java -jar target/LogGenerator-1.1-1.jar -i string -t once -fr "{oneOf:A,B,C,D}" -o cmd`

### Output modules
These are the output modules available:
- Write to file
- Write to console
- Write to UDP
- Write to TCP
- Write to TCP with SSL
- Write to Kafka
- Write to Elastic
- Write to null (throw away the result, used for performance testing)

#### Write to file
Write the received events to a local file.

Parameters: `-o file --name {file name}`

Example: `-o file --name ./received.txt`

#### Write to console
Write the received events to the console. 

Parameters: `-o cmd`

Example: `-o cmd`>

#### Write to UDP
Send events with UDP.

Parameters: `-o udp --hostname hostname --port port`

Example `-o udp --hostname localhost --port 5999`
Example `-o udp --hostname localhost -p 5999`

#### Write to TCP
Send events with TCP.

Parameters: `-o tcp --hostname hostname --port port`

Example `-o tcp --name localhost -p 5999`

### Write to TCP SSL
Send events with encrypted TCP.

Parameters and example, see below in the Q&A section.

#### Write to Kafka
Connect to a Kafka server and write the events to a topic.

Parameters: `-o kafka --client-id {client id} --topic {topic name} --boostrap-server {boostrap server}`

Example: `-o kafka --client-id test --topic testtopic --bootstrap-server localhost:9092`
Example: `-o kafka -ci test -t testtopic -b localhost:9092`

#### Write to Elastic
Connect to an Elasticsearch instance and write events to an index.

The main use case is to be able to generate events with _id set to some enumerable string, e.g., test-33.

Parameters `-o elastic --hostname {hostname or ip} --port {port number} --index {index} --api-key {API Key} --regex {regex to find an id from input} --id {format for output id} --certificate-path {X.509 certificate for the elastic server in cer format}`

``` properties
# Elasticsearch
output=elastic
# Elastic instance to connect to
hostname=192.168.1.131
# Port number
port=9200
# Index to write to
index=testindex
# The API key to use
api-key=QTNTTzM0b0ItZ0x3UEpJTTh1Z0k6cEF6Zk42NGhSdEcwUTFpYWE2Y0hBQQ==
# Pattern to use to find the id of the incoming logs
regex=(\d+)$
# String to use as an id when sending logs to elastic. ${id} will be exchanged for the value from the regex above for each log
id=testsystem-${id}
# cer file of the elastic server
certificate-path=./target/elastic.cer
# Don't generate guards
statistics=false
```

#### Write to null
This will throw away the result. It is useful, e.g., when testing for performance.

Parameters: `-o null`

Example: `-o null`

To send the text "test" 100.000 times over UDP and discard the result, but to see the eps and bps, use:

Server:

`java -jar LogGenerator-{version}.jar -i udp -p 9999 -f gap -r "(\d+)$" -o cmd -s true`

In another command window, start the client (same jar file):

`java -jar LogGenerator-{version}.jar -i counter --string "test" -o udp --hostname localhost --port 9999  -s true --limit 1000`

N.B., if you send more than approximately 50000 events, the server might begin to drop events. This is mostly because the UDP protocol is not reliable
and the receiving buffers might have to be adjusted. You can use LogGenerator to stress the UDP receiver and adjust the operating system to minimize
the number of dropped events. If you want to limit the event generation, use the event throttling setting.

### Filter modules:
- Add a header
- Replace by regex
- Select by regex
- Drop by regex
- Replace variables
- Remove transaction messages
- Detect gaps
- Extract JSON

#### Add a header
To send a file line by line but to each line prepend a header, that can contain text and variables.

Parameters: `-f header --string {header text} [--time-offset {offset in milliseconds}]`

Example: `-f header --string {syslog-header}`, `-f header --string  "My custom header with date: {date:yyyyMMdd}: "`

Time-offset is used if you want to change date generation to a date that is not the current date. For example, if you want to send events with a date that is one day ago, use `-f header --string "My custom header with date: {date:yyyyMMdd}: " --time-offset -86400000`

### Select by regex
If you want to filter out everything but a specific part of an event, you can use the SelectFilter.
Specify what to keep with a group in a regular expression.

Parameters: `-f select --regex {regex with a capture group to keep}`

Example: `-f select --regex  '-(\d+)$'`

If you have events that are formatted like this:

`[Sat Dec 03 00:35:57.399 Usb Host Notification Apple80211Set: seqNum 5460 Total 1 chg 0 en0]`

and for example want to extract the first word after the timestamp, you can use the following parameters:

`-f select --regex '\.\d{3} (\S+) '`

### Drop by regex
If you want to drop an event depending on a regular expression, use the DropFilter.
Use a regex to specify the content to search for. If the DropFilter finds a match, the event will be dropped.

Parameters: `-f drop --regex {regex}`

Example: `-f drop --regex 'INFO'`

If you have events that are formatted like this:

`[Sat Dec 03 00:35:57.399 Usb Host Notification Apple80211Set: seqNum 5460 Total 1 chg 0 en0]`

and for example want to discard all events that contain `chg 0`, you can use the following parameters:

`-f drop --regex 'chg 0'`

#### Replace by regex
A use case is if you have a lot of nice logs, but the date is not possible to use. You can load the file and add a regex to find the date, then replace the date with a {date:...} variable or a static string.
There must be a capture group in the regex. The text matched by the capture group will be replaced by the value.

Parameters: `-f regex --regex {regex to find} --value {value to insert instead of the part matched by the regex} [--time-offset {offset in milliseconds}]`

Example: `-f regex --regex "<(\d+)>" --value "<2>"`

If you have a file with a lot of logs, like:

`[Sat Dec 03 00:35:57.399 Usb Host Notification Apple80211Set: seqNum 5460 Total 1 chg 0 en0]`

Then the following invocation will change the date to today:

`java -jar LogGenerator-{version}.jar -i file --name src/test/data/log-with-time.log -s true -l 10 -f regex --regex "([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})" --value "{date:EEE MMM HH:mm:ss.sss}" -o cmd`

You can also use the --time-offset to change the date to a date that is not the current date. For example, if you want to send events with a date that is one day ago, use `-f regex --regex "([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})" --value "{date:EEE MMM HH:mm:ss.sss}" --time-offset -86400000`

There is also a way to wrap the result. E.g., if you have a string
`
TestString
`
and want to wrap that in json, you can use
```json
java -jar LogGenerator-{version}.jar -i static --string TestString -f regex --regex "^.*$" --value "{\"parameter\": \"{all}\"}" -l 1 -o cmd
```
Result:
```json
{"parameter": "TestString"}
```

in case the static string contains quotes, then use a regex filter to change those first:
```json
java -jar LogGenerator-{version}.jar -i static --string "{\"parameter\":\"TestString\"}" -f regex --regex "\"" --value "\\\\\"" -f regex --regex "^.*$" --value "{\"parameter\": \"{all}\"}" -l 1 -o cmd
```
resulting in:
```json
{"parameter": "{\"a\":\"TestString\"}"}
```


#### Replace variables
Variable substitution will be present for template, regex and header processing. If a file is loaded as "file" or template "none" then the (processor intensive) substitutions will not be loaded.

#### Remove transaction messages
If the statistics module was used when generating messages, and you want to remove them, use the Remove Guard filter on the receiver LogFilter.

Parameters: `-f guard`

Example: `-f guard`

#### Extract JSON
If an event is in JSON format, the json-filter can be used to extract a field from the input.

Parameters: `-f json --path {path to the field to extract}`

Example: `-f json --path hits.hits`

If the target is an object, the object is returned as the event. If the target is a simple datatype, the contents of the data type is returned. If the object is an array, the array is deconstructed and sent as one event for each item in the array.

Example. Get all messages from an Elastic query (here from file):

`java -jar target/LogGenerator-{version}.jar -i json-file --name src/test/data/elasticsearch.json -f json --path hits.hits -f json --path _source.message  -o cmd`

We have to read the file as a json file, else each line in the file will be an event. Then we get the hits.hits array. Each element in the array is sent as an event. Then we get the _source.message field from each element (which is the payload we want to use).

#### Detect gaps
Gaps are a continuous block of missing numbers. We use gaps to inform the filter that we are missing some events.
The GapDetector will inspect each event, search for an identification number by a regex and then check if the event has been seen before.
If an event is missing then this will be reported after the end of processing.

Parameters `-f gap --regex {regex with caputure group}`
Optional parameters
``` 
-dd   --duplicate-detection          If gap-detection is enabled, use this flag to also get a report on all serial numbers that occurs mote than once. Valid values are: false, true
-j    --json                         Should the gap detector report in JSON format instead of printable format? Valid values are: false, true
```

Example: `-f gap --regex "<(\d+)> -dd true -j true"`

A good use of gap detection is to send events over unreliable media and check if all events were delivered.
To assure that the gap detection is on, start the counter on the sending side with a number that is larger than 1. In that case, the gap detector will produce at least one gap (1-your start number).

A special use case is to continuously monitor for missed events and also to be able to react to events that come out of order and not report them as missing.
For example, log events are generated with:

`java -jar LogGenerator.jar -i counter --string test- -o udp --hostname localhost --port 9999 -e 10 -s true`

`-e 100` will limit the generated events to 100 every second.

You can now start a server and monitor for missing events. 

`java -jar LogGenerator.jar -i udp --port 9999 -f gap --regex '-(\d+)$' -c true -o null -s true`

If you start the server after the client you will have some missing events (after 30 seconds), for example:
``` 
Transaction:  transferred 439 lines in 43939 milliseconds, 0,010 kEPS 0,001 MBPS
Gaps found: 1.
1-442
Number of unique received numbers: 439
Next expected number: 882
```
If you now restart the client and wait another 30 seconds, you should se a smaller gap, or none at all.
```
Transaction transfer: TRANSACTION transferred 285 lines in 28391 milliseconds, 0,010 kEPS 0,001 MBPS
Gaps found: 1.
286-442
Number of unique received numbers: 724
Next expected number: 882
```
So, the gap detection can detect events that should have been delivered earlier, and remove them from the gaps.

### Variables
#### Date
A Date variable will take the current time and format according to a Java 
date format string. Epoch timestamps are supported but only the version with 13 digits.

Syntax: `{date:(?<datepattern>[yYmMHhsz+-dD:\d'T. ]+|epoch)(/(?<locale>[^}]+))?}`

Example (at new year 2023)
- `{date:yyyyMMdd}` might be resolved to: `20230101`
- `{date:MMM dd HH:mm:ss/en:US}` might be resolved to `Jan 01 00:00:00`
- `{date:epoch}` might be resolved to e.g., `0946681200000` (for Jan 01, 2000)

Date can also have an offset with the `-to` (`--time-offset`) parameter. Time offset can be used to add or remove a number of milliseconds from the date variable. 

Syntax: `-to` [offset in milliseconds]

Example: `-to -86400000` to set the date variable to one day ago.

#### Ipv4
Insert an ip address from a specified subnet.

Syntax: `{ipv4:(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})/(\d{1,2})}`

Example: 
- `{ipv4:192.168.1.182/24}` might be resolved to `192.168.1.14`

### Ipv6
A very simple variable substitution exists for generating ipv6-format.
The variable is defined as: `{repeat:8#{string:0-9a-f/4}#-#}`

Syntax: `{ipv6}`

Example:
- `eaf1-65f0-f0df-2d39-9657-6cf2-9d29-9825`

#### OneOf
This variable will randomly pick one of the supplied arguments.

Syntax: `{oneOf:(?<options>.*)(/(?<delimiter>.))?}`

Example:
- `{oneOf:a,b,c,d}` will be replaced by one of a, b, c or d
- `{oneOf:a;b;c;d/;}` if , is needed in the options, a delimiter can be provided in the template.

#### OneFromFile
OneFromFile works a bit like oneOf except the values come from an external file and the delimiter is not used. 
Each line in the file will be a new option for oneFromFile. 

Syntax: {oneFromFile:(?<filename>[^#]+)(#(?<encoding>.*))?}

Example: 
- `{oneFromFile:/home/user/Desktop/test.txt}`
- `{oneFromFile:/home/user/Desktop/test.txt#ISO-8859-1}`


### Prob
This is a variant of the OneOf variable, with an option to set the relative probability for picking one of the choices, but without the delimiter option.

Syntax: `{prob:(?<options>.*)}` where the options are a comma separated list of `(?<name>[^/]+)(/(?<probability>\d+))?`

Example:
- `{prob:a,b,c,d}` will work as `{oneOf:a,b,c,d}`
- `{prob:a,b/2,c/100,d}` will first sum the relative probabilities: 1 + 2 + 100 + 1 = 104, then generate a random number between 1 and 104 and lastly pick the corresponding string from the list. 
The possibility of choosing c is high, but a, b and d will be occasionally selected.

#### Random
A Random variable will pick a random number in the specified interval (included).

Syntax: `{random:(?<from>\d+)-(?<to>\d+)}`

Example: `{random:1-6}` will be evaluated to 1, 2, 3, 4, 5 or 6.

#### String
A string made from a number of characters. The characters to choose from and the length of the string is specified.

Syntax: `{string:(?<characters>[^/]+)/(?<numberof>\d+)}`

Example: `{string:a-c/8`} will for example create `aaaccaca`

The first argument can be for example a-zA-Z0-9_\- to include letters, numbers, the underscore _ and the hyphen - characters.

#### Repeat
A repeat variable will repeat a string a number of times.

Syntax: `{repeat:(?<times>[^#]+)#(?<torepeat>[^#]+)(#(?<delimiter>[^#]+))?#}`

Example: `{repeat:5#a#}` will be substituted with `aaaaa`

Example: `{repeat:5#a#-#}` will be substituted with `a-a-a-a-a`

Example: `{repeat:{random:1-7}}#b#}` might be substituted with one to seven `b` 

The <torepeat> field can be another variable.

#### Lorem.
The Lorem variable will pick a number of random word from a list of words and add with a delimiter between.

Syntax: `{lorem:(?<length>\d+):(?<wordlist>.*)/(?<delimiter>.)}`

Example: `{lorem:4:this is a nice story of random words that will be assembled but just four of the words/ }` 
can for example be substituted with `words nice words is`.

#### Counter
A counter is an integer with a starting value that will be substituted with the value of the counter on the first invocation. 
On subsequent invocations, the value is increased by 1 for each invocation.

A counter can be named, so different counters may have different values. If no name is given, a default name `defaultName` will be used for that counter.

To differentiate between counters each counter has its own unique name. 

Syntax: `{counter:((?<name>[a-zA-Z0-9\-_]+):)?(?<startvalue>\d+)}`

Example: 
`{counter:counterName:14}` will be substituted by `14` the first time the template is evaluated, `15` the next time and so on.

`{counter:1}` will be substituted by `1` the first time the template is evaluated, `2` the next time and so on.

#### CounterMemoryRecall
Each time a counter is used, the counter value is automatically saved in a cache. If you want to use the same value
again you can access the last value of a specific counter.

Syntax: `{cmr(:(?<name>[a-zA-Z0-9\\-_]+))?}"`

Example:
`{cmr}` will be substituted by the last number of the default counter

`{cmr:myname}` will be substituted by the last number of the counter with the name `myname`

#### Pri
A pri will just create a random facility, severity and priority for a syslog message and
return the priority value that is used in the syslog header.

Syntax: `{pri:}`

Example: `{pri:}` might be substituted by `165`

#### MemorySet, MemoryRecall
The MemorySet will not create any output, but only save the value in the internal cache. The payload of the MemorySet command will create a payload as usual. After an expression has been evaluated, the value can be retrieved with the MemoryRecall variable.

Syntax: `{ms(:(?<name>[a-zA-Z0-9\-_]+))?/(?<value>.*)}`
Syntax: 

Example:
Save the value TEST in the default memory:
- `{ms/TEST}`

Retrieve the last saved default memory:
- `{mr}`

Save the value gained from {oneOf:a,b,c,d} in a variable with the name `other`. Also, output the value from that variable.
This will evaluate to `a a`, `b b`, `c c` or `d d`.
- `{ms:other/{oneOf:a,b,c,e}} {mr:other}`

## Order of substitution
The substitutions will process one after another and repeat until the result is stable, so the output of one filter can be the input to another.

Example: `{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}` will evaluate to a random RFC 1918 address.

## Usage
### Read a file and print the output to the console

`java -jar LogGenerator-{version}.jar -i file --name file.txt -o cmd`

### Read a file and send the output to a UDP listener

`java -jar LogGenerator-{version}.jar -i file --name file.txt -o udp --hostname localhost --port 514`

### Read a file, add a syslog header and send the output to the console
This will add a syslog header to each line in the file before printing the line.

`java -jar LogGenerator-{version}.jar -i file --name test.txt -f header -st "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: " -o cmd`

Example: 
- `<25>Dec 10 15:27:38 192.168.169.209 liiblhukp[38946]: Test row 1`
- `<165>Dec 10 15:27:38 localhost 0th3ih5rq[13347]: Test row 2`

### Read a file, change the date from the lines in the file to the date right now and print to the console

Example: If a log looks like this: 

`Sat Dec 03 00:35:57.108 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0`

`java -jar LogGenerator-{version}.jar -i file --name log-with-time.log -f regex --regex "[a-zA-Z]{3} [a-zA-Z]{3} \d{1,2} \d\d:\d\d:\d\d\.\d{3}" --value "{date:EEE MMM dd HH:mm:ss}" -o cmd`

might print 

`Sat Dec 10 16:02:24 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0` (note the date and time change)

## Templates
So far, we have only used variables in headers, but we can also use variables in the files. The template engine will only load if one of these conditions are met:

- The regex parameter is set on the command line
- The header parameter is set on the command line
- The template parameter is set to something other than 'none'

The templates use regexes so if we can avoid to load and run them on each entry we can save a lot of processing power. 
For applications that need lots of eps, try generating the events to file first and then use LogGenerator or other application to load from disk.

### Template files
A template file is just a file with one or more lines that contain text and/or variables ({date: ...). Templates can be run as:

- none: meaning the file will just be processed as usual but in a random order (if not regex or header is used at the same time)
- file: read one random line from the file and send to the filter output. The line will not be read again. When the last line has been sent, the program halts.
- continuous: read one random line from the file and send to the filter output. The line might be sent again.
- time:x run for x number of milliseconds. read one random line from the file and send to the filter output. Example: `-t time:5000` to run for 5 seconds

Example. We have a template file with two lines:

- `row1 {oneOf:a,b,c,d}`
- `row2 {random:10-13}`

The result from running: 

`java -jar LogGenerator-{version}.jar -i template --template continuous --name template.txt -o cmd  -s true -l 10000`

will be 10.000 lines beginning with `row1` or `row2` and ending with a letter a-d or number 10-13.

If a fixed time is preferred, use `--template time:durationInMilliseconds`, example `--template time:10000` to process for 10 seconds.

With a syslog template, a great number of unique logs can be generated.

## Configuration files
Instead of passing all parameters on the command line, one parameter (-p configuration-file) can be used instead.
A combination of parameters from the command line and property file is possible.

The property file can have all short- or long names for configuration. Comment lines start with the hash character '#'.

Example:
```properties
# Start an udp proxy, listening on port 9999, rewriting dates to the current date. 
# Dates are parsed  according to a specified format and finally sending the result
# to a syslog server.

# Listen to all interfaces, port 9999
input=udp
port=9999

# Rewrite dates that are formatted like: Sat Dec 03 00:34:34.362
filter=regex
regex=([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})
# with today's date in this format
value={date:EEE MMM dd HH:mm:ss.sss}

# And send to a syslog server
output=udp
hostname=localhost
port=514
```

Use the above property file:

`java -jar LogGenerator-{version}.jar -pf <name-of-property-file>`

### System variables
Some variables are built-in, expanding to other variables and thus easier to use.
To use the syslog-header built-in variable, add `{syslog-header}` to either the template file or the header command line argument. 

The following system variables can be used:

- syslog-header: `<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: `
- ip: `{ipv4:0.0.0.0/0}`
- rfc1918: `{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}`

### Custom variables
In addition to the system variables, you can add a set of custom variables. Custom variables are text snippets that will be expanded to some value when the filtering is performed.

There are three ways to add custom variables:

1. On the command line, add `-vf` or `--variable-file`, and a name of a property file containing variable-name=value.
2. In a property file added with `-pf` or `--property-file`, add `variable-file=<path to a property file>`, e.g., `variable-file=src/test/data/variables.properties`
3. In a property file added with `-pf` or `--property-file`, add `custom.` and directly after that the variable-name=value, e.g., `custom.myIP={ipv4:192.168.1.0/24}`

Example:
Say you would like to always exchange the text `{company-name}` in a template with `sitia.nu`. Then add the following to a property file (let's call it variables.properties):
```properties
company-name=sitia.nu
```
and add that file to the property file loaded by `-pf` on the command to LogGenerator:
```properties
variable-file=variables.properties
```
If you only have a small number of custom variables, you could just load all of them in the property file loaded by `-p`:
```properties
custom.company-name=sitia.nu
```
Custom variables can contain variables.

## Chaining LogGenerator 
Now that we have an understanding of the basics, we can progress to the more advanced use cases for LogGenerator.
To locate bottlenecks or bad connections in a log chain, one or more components can be substituted with LogGenerator to create a known set of data to send and receive.

LogGenerator can be used to generate logs and receive logs, count them, calculate events per second and even detect lost events.

You can use several LogGenerators inserted at different places in your event stream.

### Lost events - Gap detection
If the events transferred contains an increasing value, then the receiver can detect if any of the values are missing.

To generate a header with that kind of value, use {counter: variable: startNumber}.

`{counter:name:1}`

Example: Send data from a file, add a header with a counter and some text around the counter, so we can identify that on the server side.
First, send to cmd, so we can see that the counter is working:

`java -jar LogGenerator-{version}.jar -i file --name src/test/data/log-with-time.log -f header --string "<{counter:test:1}>" -o cmd`

We should se some events starting with <1>, <2> etc. Since we'd like to be able to see if the Gap Detection is working, 
set the initial value to, e.g., 42.

Example with only one instance of LogGenerator:

Server and client:

`java -jar LogGenerator-with-dependencies.jar -i file --name src/test/data/test.txt -f header --string "<{counter:test:42}>" -f gap --regex "<(\d+)>" -o cmd`

You should see the received data and the detected gaps:

```
[<42>Test row 1]
[<43>Test row 2]
[<44>Test row 3]
[<45>Test row 4]
Number of unique received numbers: 4
Next expected number: 46
Gaps found: 1.
1-41

```

Example with one client and one server:

In a new terminal, start the server:
`java -jar LogGenerator-{version}.jar -i tcp --port 9999 -f gap --regex "<(\d+)>" -o cmd`

Start the client:

`java -jar LogGenerator-{version}.jar -i file --name src/test/data/test.txt -f header --string "<{counter:test:42}>" -o tcp --hostname localhost --port 9999`

You should see the received data. To see the gaps, press Ctrl-C:

```
INFO: Serving TCP server on port: 9999
[<42>Test row 1]
[<43>Test row 2]
[<44>Test row 3]
[<45>Test row 4]
^CSigint
Duplicate detection found: 0 duplicate (or more) values.
Number of unique received numbers: 4
Next expected number: 46
Gaps found: 1.
1-41
```

Now it's easy to configure the first LogGenerator to send logs to, e.g., Kafka or rsyslog and connect the LogGenerator receiver to accept UDP connections or read from Kafka.
By sending more data and possibly throttle the data, you can test how much load different parts of the log chain can handle.

### Duplicates detection
When using gap detection, the flag `-dd` can be used to also report on events that are received more than once.

Syntax: `-dd true` `--duplicate-detection true`

Example, client: `java -jar LogGenerator-{version}.jar -i file --name src/test/data/test.txt -f header --string "<{counter:test:42}>" -f gap --regex "<(\d+)>" -dd true -o tcp --hostname localhost --port 9999`
Server: `java -jar LogGenerator-{version}.jar -i tcp --port 9999 -f gap --regex "<(\d+)>" -dd true -o cmd`

## Improving performance
First, regexes can be really slow. If you want good performance, use the static or counter input module and no headers. 
A good way to improve performance is to create a lot of events in a file or Kafka, with all the bells and whistles of the template regexes and then use that precompiled data in the performance tuning.

Also, you can batch some input and output items in mini batches that will sometimes improve the sending and receiving rate. A batch is one event with a number of lines, separated by newline-character.

`--batch-size {number of events in each batch}`


## Q&A
### Why does my cmd printout have square brackets around every line?
When using the -o cmd each batch of events will be printed on one line, separated by , and with [] around the batch. 
This behaviour is to illustrate the batch mechanism and will not be present if you write to file with `-o file --name {filename}`.

### What is a good regex to use with the -i counter module?
A regex could be `"(\d+)$"` since $ denotes end-of-string.

Example:

`java -jar LogGenerator-{version}.jar -i counter --string "Test:" --limit 100000 -f gap --regex "(\d+)$" -o null -s true`

### This sounds nice and all, but how do I start? Can I get the built jar?
From version 1.02, the jar will be available on the download page.

### Build instructions
For building the jar, use the following commands:

`git clone https://github.com/anders-wartoft/LogGenerator.git`

`cd LogGenerator`

`mvn clean package`

The jar is now in a directory called `target`.

Note that the package is developed with Java 11.

### I want to enable logging, how do I do that?
Logging can be enabled by adding instructions to the logging framework. The amount of logging is not great though.

Start the program as usual, but add `-Djava.util.logging.config.file=logging.properties` to java. 

Example: `java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-{version}.jar -i udp --hostname localhost --port 9999 -o cmd  -s true -l 100`

### How do I send and receive from Kafka?
Example of sending a few lines to Kafka:

Server:

`java -Djava.util.logging.config.file=logging.properties -jar LogGenerator-{version}.jar -i kafka -ci testclient --topic test -b 192.168.1.116:9092  -f gap --regex "Test:(\d+)$" -s true -o cmd `

Client:

`java -jar target/LogGenerator-{version}.jar -i counter --string "Test:" -o kafka -ci test2 --topic test -b 192.168.1.116:9092 --batch-size 10 --limit 100  -s true `

### How do I get a cer file for the Elastic module (input or output)?
There are several ways. You can use openssl from the command line but the simplest way is to open a browser and navigate to
`https://hostname-for-elastic-instance:9200`. The procedure is a bit different for different browser, but basically:
when the page is displayed, either a padlock or a triangle with an exclamation mark inside is visible in the address field of the browser.
Right-click on the padlock/triangle, choose certificate, info and Export... You can now save the certificate and use with the parameter -eoc or -eic.

### How do I get other data than _id from the Elastic input module?
First, remove the _source from the query. _source will determine what fields are returned from the query. 
Optionally, you can set the `elastic-input-field=_source` to retrieve all fields from the query but remove everything from the search.

In the query, instead of this:
``` json
{"query": { "query_string": { "query": "*" }}, "_source": ["_id"]}
```
Just remove the _source:
``` json
{"query": { "query_string": { "query": "*" }}}
```
Now, you will get lines like this from Elastic:
``` 
[{"_index":"testindex","_id":"test2-11","_score":1.0,"_source":{"@timestamp":"2023-09-29T20:24:29","message":"Test row 11"}}]]
```

If you are only interested in the _source, set `--field _source`. The result will now become:
``` 
[{"@timestamp":"2023-09-29T19:08:52","message":"Test row 11"}]
```
Note that the _id is not in the returned content here. For further selection, use a RegexFilter or a json-filter for processing.
If the size of the data is large, you might want to consider just getting the _id with the _source parameter to minimize the impact on the cluster.

``` json
{"query": { "query_string": { "query": "*" }}, "_source": ["_id"]}
```
and in the property file:
`field=_source`
Might give the following output:
``` 
[myid-1]
[myid-2]
[myid-3]
[myid-4]
```
Used in conjunction with a gap detector, the LogGenerator can verify that all logs from a source (that has an enumerable field) is stored in the Elastic instance once and only once.

### Whats the --------BEGIN_TRANSACTION-------- for?
Those are messages inserted into the event stream to be able to detect start of transfers and to save a timestamp for the statistics module to work.
They are generated by some module but only if `-s true` is set (statistics).

### This is great but I need feature xxx
This is open source, so "Use the Source, Luke". 

### The variables are not expanded. They are delivered as {date:... and not as 2023-01-01...
On the sending side, use the template file input `-i template --template file` or similar (see above).

Example: to send random data from a template file to an udp listener on port 9999 for 5 seconds, use:

`java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-{version}.jar -i template --name src/test/data/template.txt --template time:5000 -o udp --hostname localhost --port 9999`

### The -e eps throttling doesn't work
Add `-s true` so that timestamps are updated.

Example: 

`java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-{version}.jar -i template --name src/test/data/template.txt -t continuous -o udp --hostname localhost --port 9999  --limit 100  -e 2 -s true`

### What about SSL. How do I get started with SSL sockets?

Follow the instructions from https://unix.stackexchange.com/questions/347116/how-to-create-keystore-and-truststore-using-self-signed-certificate

To generate certificate and keystores for localhost, you can use these commands. `keytool` comes with Java.
```bash
keytool -genkey -keyalg RSA -keypass changeit -storepass changeit -keystore server.keystore
keytool -export -storepass changeit -file server.cer -keystore server.keystore
keytool -import -v -trustcacerts -file server.cer -keystore server.truststore -keypass changeit -storepass changeit
openssl x509 -inform der -in server.cer -out server.pem
```
N.B. Common Name (CN) must be equal to the server name.

To examine the certificate:`keytool -printcert -v -file server.cer`

If you get stuck, this is a nice reference to debugging ssl: help: https://stackoverflow.com/questions/17742003/how-to-debug-ssl-handshake-using-curl#22814663

You can also use the supplied keystore and truststore in src/test/data/certs. Both have the password: `changeit`. Also, check out the configuration file src/test/data/certs/ssl-server.properties for a working setup.

To test the ssl connection, first copy the key- and truststore to the root of LogGenerator:

LogGenerator % `cp src/test/data/certs/server.* . `

Start a server (add the keystore and password as java runtime parameter):

`java -Djavax.net.ssl.keyStore=server.keystore -Djavax.net.ssl.keyStorePassword=changeit -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -p src/test/data/certs/ssl-server.properties`

The keystore and truststore must be in the same directory that you start the jar from, otherwise you will have to add an ABSOLUTE path to the keystores. Java will not recognize relative paths for keystores.

From another console window, start the client (add the truststore and password):

`java -Djavax.net.ssl.trustStore=server.truststore -Djavax.net.ssl.trustStorePassword=changeit -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -p src/test/data/certs/ssl-client.properties`

Debugging

You can debug the input or output items independently. openssl and curl has nice features for ssl debugging.

From another console window, use either of these clients to connect to the server:
```bash
openssl s_client -connect localhost:9999 -tls1_2 -status -msg -debug
openssl s_client -connect localhost:9999 -tls1_2 -status -msg < src/test/data/test.txt
ncat --ssl localhost 9999 < src/test/data/test.txt
curl -iv https://localhost:9999
```
or any browser with https://localhost:9999 (you will have to accept the certificate if the issuer path is not in the browser's truststore).

Note that ncat can both do ssl and delays (throttle) so that can be a nice tool to send prefabricated events with. 
Start by creating a large amount of events from a template and store in a file. Then use nc, ncat or similar to deliver the files, if the
built-in networking is too slow.

To start a debugging SSL server with openssl, first generate a .p12 file from the keystore:
```bash
keytool -importkeystore -srckeystore server.keystore -destkeystore keystore.p12 -deststoretype PKCS12 -srcstorepass changei
openssl pkcs12 -in keystore.p12 -out server.key -nodes -nocerts
```

Now you should be able to start an openssl server with:

`openssl s_server -CAfile server.cer -accept 9999 -key server.key`

Start by browsing to https://localhost:9999 to see if the openssl server is responding. The browser should wait for a response, but the server should write the HTTP header (GET / HTTP/1.1 ...) on the console.

For more debugging information, you can start the openssl server with:

`openssl s_server -CAfile server.cer -accept 9999 -key server.key -debug`

To start java with additional logging for ssl debugging, use:

`-Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager -Djava.security.debug=access:stack`

### How do I know if a TCP input is working
If you have started a TCP input with `-i tcp -ip 9999 -o cmd` then you can send data with 
`nc {ip-address or name} {port} < {filename}`, e.g., `nc localhost 9999 < /var/log/messages`.
An even easier method is to use an Internet Browser and open http://localhost:9999. The browser won't receive any data but the command window should write a few lines beginning with GET, Host, User-Agent and similar.

### Gap Detection doesn't work when I use a property file
On the command line you can escape the backslash character, so a pattern would be:
``` bash
-f gap --regex "<(\\d+)>"
```
Also, you can omit the escape:
``` bash
-f gap --regex "<(\d+)>"
```
But, in the property file you have to write the regex as is, without extra escape characters, like
```properties
filter=gap
regex=<(\d+)>
```
There's an example in ssl-server.properties.

### What happens if I use a name for the standard variables for my custom variable
If there is a standard variable with a name of, e.g., `ip`, and you define a new variable with the same name, your variable definition will overwrite the standard variable.

## What License are you using?
See the license header in each java file. As long as you don't violate the licenses of the components (kafka and slf4j), you can do whatever you want with the code, just give me credit if you use the code.

