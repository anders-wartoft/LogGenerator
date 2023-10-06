# LogGenerator
LogGenerator is a tool to debug log streams, i.e., syslog, Kafka, UDP diodes and similar chains of log collection systems.
The tool reads input from an input module, filters the input (add a header, replace text etc.) and finally writes the output with an output module.

LogGenerator uses input modules, filters and output modules and combines that into a chain. Each event from the input module is processed by zero or more filters, that can rewrite the contents. After filtration the events are written with an output module.

Example: Read a log file, add a syslog header and write to a remote Kafka. In the syslog header, add a counter that starts at 100 and increases with each string. Also, add statistics messages (beginning of transaction etc.).
When the events are stored in Kafka, start another LogGenerator that fetches the Kafka events, checks the counter and writes the events to null (to increase performance). Give a measurement of the time elapsed, how many items were received, the event per second and an estimate of the bandwidth usage as well as a list of missed events (counter integers that are missing) and the next counter number we are expecting.

The example above is literally two commands. 

```bash
java -jar target/LogGenerator-with-dependencies.jar -i file -ifn src/test/data/test.txt -o kafka -ocn test2 -otn test -obs 192.168.1.116:9092 -he "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{counter:a:100}]: " -s true
java -jar target/LogGenerator-with-dependencies.jar -i kafka -icn test2 -itn test -ibs 192.168.1.116:9092 -gd "\[(\d+)\]:" -o null -s true
```
When running the last command, press Ctrl-C to see the gaps in the received data. Since we started the counter on 100, there should at least be one gap: 1-99.

### Release notes
#### 1.02
- Bug fixes for Elastic Input Item
- Added Json File Input Item
- Json- and Elastic items now emit found array items as new events
- Refactored the code to run on Java 11
- Changed maven script to Java 11
- Bug fixes in Json Filter. You can now extract a field with the dot (.) notation, e.g., _source.message

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

#### Read files
Read a local file

Parameters: `-i file -ifn {file name}`

Example: `-i file -ifn ./src/test/data/test.txt`

#### Read files in a directory
Read all files in a directory

Parameters: `-i file -ifn {directory name}`

Example: `-i file -ifn ./src/test/data/`

#### Read files in a directory with globs
Read all files in a directory that matches a glob. See https://javapapers.com/java/glob-with-java-nio/ for details on how to write globs.

Parameters: `-i file -ifn {directory name -g "{glob}"`). 

Example: `-i file -ifn ./src/test/data/ -g "**.txt"`

Note that all * must be within quotes, since otherwise, the OS will expand that variable.

#### Read JSON file
If a file is in JSON format (not line json but the entire file is one JSON object) you can read the file with the JSON File input.

Parameters: `-i json-file -ifn {filename}`

Example: `-i json-file -ifn ./src/test/data/elasticsearch.json`

If the file contains an array you would like to extract, use the parameter `-jfp` `--json-file-path`.
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
To read this response from file, use the `json-file` input and set `-jfp` to `hits.hits`. The result will be an array of elements and each element will be emitted as a new event.
So, to extract the `_id` from each element, add a `json-filter` with `-jf _id`. Now only the `_id` field will be propagated.

The command line will then become:
`java -jar LogGenerator-with-dependencies.jar -i json-file -ifn ./src/test/data/elasticsearch.json -jfp hits.hits -jf _id -o cmd`

#### Receive UDP
Set up a UDP server. 

Parameters: `-i udp [-ih {host}][ -ip portnumber`

Example: `-i udp -ih localhost -ip 5999` or `-i udp -ip 5999`

#### Receive TCP
Set up a TCP server. 

Parameters: `-i tcp [-ih {host}] -ip portnumber`

Example: `-i tcp -ih 192.168.1.2 -ip 5999` or `-i tcp -ip 5999`

### Receive TCP SSL
Set up a TCP server with encrypted communication.

Parameters and example, see below in the Q&A section.

#### Fetch from Kafka topics
Connect to a Kafka server and read from a topic

Parameters: `-i kafka -cn {client name} -tn {topic name} -bs {boostrap server}`

Example: `-i kafka -cn test -tn testtopic -bs localhost:9092`

#### Fetch from Elasticsearch index
Connect to an Elasticsearch instance and read from an index:

The main use case is to get one field from an index and send to a receiver, for example a gap detector to be able to see if there are missing events (or duplicates) in the index.

Parameters: `-i elastic -eih {hostname or ip} -eip {port number} -eii {index name} -eif {field to get} -eiak {API key} -eic {x.509 certificate in cer format} -eiq {query string in query dsl format}`

Example:
``` properties
# Elasticsearch
input=elastic
output=cmd
# Elastic instance to connect to
elastic-input-host=192.168.1.131
# Port number
elastic-input-port=9200
# Index to read from
elastic-input-index=testindex
# The API key to use
elastic-input-api-key=QTNTTzM0b0ItZ0x3UEpJTTh1Z0k6cEF6Zk42NGhSdEcwUTFpYWE2Y0hBQQ==
# Field to use for gap detection
elastic-input-field=_id
# cer file of the elastic server
elastic-input-cer=./target/elastic.cer
# Query for the input item
elastic-input-query={"query": { "query_string": { "query": "*" }}, "_source": ["_id"]}
# How many items to fetch from Elastic in each REST request
input-batch-size=1000
# Detect gaps in the id:s. Since we extract only the _id field, this should suffice
gap-detection=-(\d+)$
# Check if we have duplicate id:s in the stream
duplicate-detection=true
# Don't generate guards
statistics=false
```

#### Static string
Send the same string over and over again. 

Parameters: `-i static -string {the string to send}`

Example: `-i static -string "Test string to send"`

Example: `-i static -string "Test string to send" -l 1000` (only send 1000 events)

#### Static string ending with a counter starting from 1
This is used to send a static string that is appended with an increasing number, starting from 1. This is a very fast way to send events ending with a counter.
To send 1.000.000 events from "Test:1" to "Test:1000000", use `java -jar LogGenerator-with-dependencies.jar -i counter -string "Test:" --limit 1000000 -o cmd`

Parameters: `-i counter -string {the string to send}`

Example: `-i counter -string "Test string number:"`

If you want to test the generation speed, use the `null` output since that is faster than writing to the console. Add `-s true` for measurements: `java -jar target/LogGenerator-with-dependencies.jar -i counter -string "Test:" --limit 1000000 -o null -s true`

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

Parameters: `-o file -ofn {file name}`

Example: `-o file -ofn ./received.txt`

#### Write to console
Write the received events to the console. 

Parameters: `-o cmd`

Example: `-o cmd`>

#### Write to UDP
Send events with UDP.

Parameters: `-o udp -oh hostname -op port`

Example `-o udp -oh localhost -op 5999`

#### Write to TCP
Send events with TCP.

Parameters: `-o tcp -oh hostname -op port`

Example `-o tcp -oh localhost -op 5999`

### Write to TCP SSL
Send events with encrypted TCP.

Parameters and example, see below in the Q&A section.

#### Write to Kafka
Connect to a Kafka server and write the events to a topic.

Parameters: `-o kafka -ocm {client name} -otn {topic name} -obs {boostrap server}`

Example: `-o kafka -ocn test -otn testtopic -obs localhost:9092`

#### Write to Elastic
Connect to an Elasticsearch instance and write events to an index.

The main use case is to be able to generate events with _id set to some enumerable string, e.g., test-33.

Parameters `-o elastic -eoh {hostname or ip} -eop {port number} -eoi {index} -eoak {API Key} -eoir {regex to find an id from input} -eoi {format for output id} -eoc {X.509 certificate for the elastic server in cer format}`

``` properties
# Elasticsearch
output=elastic
# Elastic instance to connect to
elastic-output-host=192.168.1.131
# Port number
elastic-output-port=9200
# Index to write to
elastic-output-index=testindex
# The API key to use
elastic-output-api-key=QTNTTzM0b0ItZ0x3UEpJTTh1Z0k6cEF6Zk42NGhSdEcwUTFpYWE2Y0hBQQ==
# Pattern to use to find the id of the incoming logs
elastic-output-id-regex=(\d+)$
# String to user as an id when sending logs to elastic. ${id} will be exchanged for the value from the elastic-output-id-regex above for each log
elastic-output-id=testsystem-${id}
# cer file of the elastic server
elastic-output-cer=./target/elastic.cer
# Don't generate guards
statistics=false
```

#### Write to null
This will throw away the result. It is useful, e.g., when testing for performance.

Parameters: `-o null`

Example: `-o null`

To send the text "test" 100.000 times over UDP and discard the result, but to see the eps and bps, use:

Server:

`java -jar LogGenerator-with-dependencies.jar -i udp -ip 9999  -o cmd  -s true  -gd "(\d+)$"`

In another command window, start the client (same jar file):

`java -jar LogGenerator-with-dependencies.jar -i counter -string "test" -o udp -oh localhost -op 9999  -s true --limit 100000`

### Filter modules:
- Add a header
- Replace by regex
- Replace variables
- Remove transaction messages
- Detect gaps
- Extract JSON

#### Add a header
To send a file line by line but to each line prepend a header, that can contain text and variables.

Parameters: `-he {header text}`

Example: `-he {syslog-header}`, `-he "My custom header with date: {date:yyyyMMdd}: "`

#### Replace by regex
A use case is if you have a lot of nice logs, but the date is not possible to use. You can load the file and add a regex to find the date, then replace the date with a {date:...} variable or a static string.
There must be a capture group in the regex. The text matched by the capture group will be replaced by the value.

Parameters: `-r {regex to find} -v {value to insert instead of the part matched by the regex}`

Example: `-r "<(\d+)>" -v "<2>"`

If you have a file with a lot of logs, like:

`[Sat Dec 03 00:35:57.399 Usb Host Notification Apple80211Set: seqNum 5460 Total 1 chg 0 en0]`

Then the following invocation will change the date to today:

`java -jar LogGenerator-with-dependencies.jar -i file -ifn src/test/data/log-with-time.log -o cmd -s true -l 10 -r "([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})" -v "{date:EEE MMM HH:mm:ss.sss}"`

#### Replace variables
Variable substitution will be present for template, regex and header processing. If a file is loaded as "file" or template "none" then the (processor intensive) substitutions will not be loaded.

#### Remove transaction messages
If the statistics module was used when generating messages, and you want to remove them, use the Remove Guard filter on the receiver LogFilter.

Parameters: `-rg` or `--remove-guard`

Example: `-rg`

#### Extract JSON
If an event is in JSON format, the json-filter can be used to extract a field from the input.

Parameters: `-jf` or `--json-filter`

Example: `-jf hits.hits`

If the target is an object, the object is returned as the event. If the target is a simple datatype, the contents of the data type is returned. If the object is an array, the array is deconstructed and sent as one event for each item in the array.

#### Detect gaps
Gaps are a continuous block of missing numbers. We use gaps to inform the filter that we are missing some events.
The GapDetector will inspect each event, search for an identification number by a regex and then check if the event has been seen before.
If an event is missing then this will be reported after the end of processing.

Parameters `-gd {regex with caputure group}`
Optional parameters
``` 
-dd   --duplicate-detection          If -gd (--gap-detection) is enabled, use this flag to also get a report on all serial numbers that occurs mote than once. Valid values are: false, true
-gdjr --gap-detection-json-report    Should the gap detector report in JSON format instead of printable format? Valid values are: false, true
```

Example: `-gd "<(\d+)> -dd true -gdjr true"`

A good use of gap detection is to send events over unreliable media and check if all events were delivered.
To assure that the gap detection is on, start the counter on the sending side with a number that is larger than 1. In that case, the gap detector will produce at least one gap (1-your start number).

### Variables
#### Date
A Date variable will take the current time and format according to a Java 
date format string. 

Syntax: `{date:(?<datepattern>[yYmMHhsz+-dD:\d'T. ]+)(/(?<locale>[^}]+))?}`

Example (at new year 2023)
- `{date:yyyyMMdd}` might be resolved to: `20230101`
- `{date:MMM dd HH:mm:ss/en:US}` might be resolved to `Jan 01 00:00:00`

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
The MemorySet will not create any output, but only save the value in the internal cache. After an expression has been
evaluated, the value can be retrieved with the MemoryRecall variable.

Syntax: `\{ms(:(?<name>[a-zA-Z0-9\-_]+))?/(?<value>.*)}`
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

`java -jar LogGenerator-with-dependencies.jar -i file -ifn file.txt -o cmd`

### Read a file and send the output to a UDP listener

`java -jar LogGenerator-with-dependencies.jar -i file -ifn file.txt -o udp -oh localhost -op 514`

### Read a file, add a syslog header and send the output to the console
This will add a syslog header to each line in the file before printing the line.

`java -jar LogGenerator-with-dependencies.jar -i file -ifn test.txt -o cmd -he "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: "`

Example: 
- `<25>Dec 10 15:27:38 192.168.169.209 liiblhukp[38946]: Test row 1`
- `<165>Dec 10 15:27:38 localhost 0th3ih5rq[13347]: Test row 2`

### Read a file, change the date from the lines in the file to the date right now and print to the console

Example: If a log looks like this: 

`Sat Dec 03 00:35:57.108 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0`

`java -jar LogGenerator-with-dependencies.jar -i file -ifn log-with-time.log -r "[a-zA-Z]{3} [a-zA-Z]{3} \d{1,2} \d\d:\d\d:\d\d\.\d{3}" -v "{date:EEE MMM dd HH:mm:ss}" -o cmd`

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

`java -jar LogGenerator-with-dependencies.jar -i template -t continuous -ifn template.txt -o cmd  -s true -l 10000`

will be 10.000 lines beginning with `row1` or `row2` and ending with a letter a-d or number 10-13.

If a fixed time is preferred, use `-t time:durationInMilliseconds`, example `-t time:10000` to process for 10 seconds.

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
input-port=9999

# Rewrite dates that are formatted like: Sat Dec 03 00:34:34.362
regex=([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})
# with today's date in this format
value={date:EEE MMM dd HH:mm:ss.sss}

# And send to a syslog server
output=udp
output-host=localhost
output-port=514
```

Use the above property file:

`java -jar LogGenerator-with-dependencies.jar -p <name-of-property-file>`

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
2. In a property file added with `-p` or `--property-file`, add `variable-file=<path to a property file>`, e.g., `variable-file=src/test/data/variables.properties`
3. In a property file added with `-p` or `--property-file`, add `custom.` and directly after that the variable-name=value, e.g., `custom.myIP={ipv4:192.168.1.0/24}`

Example:
Say you would like to always exchange the text `{company-name}` in a template with `sitia.nu`. Then add the following to a property file (let's call it variables.properties):
```properties
company-name=sitia.nu
```
and add that file to the property file loaded by `-p` on the command to LogGenerator:
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

`java -jar LogGenerator-with-dependencies.jar -i file -ifn src/test/data/log-with-time.log -he "<{counter:test:1}>" -o cmd`

We should se some events starting with <1>, <2> etc. Since we'd like to be able to see if the Gap Detection is working, 
set the initial value to, e.g., 42.

Example with only one instance of LogGenerator:

Server and client:

`java -jar LogGenerator-with-dependencies.jar -i file -ifn src/test/data/test.txt -he "<{counter:test:42}>" -o cmd -gd "<(\d+)>"`

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
`java -jar LogGenerator-with-dependencies.jar -i tcp -ip 9999 -o cmd  -gd "<(\d+)>"`

Start the client:

`java -jar LogGenerator-with-dependencies.jar -i file -ifn src/test/data/test.txt -he "<{counter:test:42}>" -o tcp -oh localhost -op 9999 -dd true`

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

Syntax: `-dd true`

Example: `java -jar LogGenerator-with-dependencies.jar -i file -ifn src/test/data/test.txt -he "<{counter:test:42}>" -o tcp -oh localhost -op 9999 -dd true`

## Improving performance
First, regexes can be really slow. If you want good performance, use the static or counter input module and no headers. 
A good way to improve performance is to create a lot of events in a file or Kafka, with all the bells and whistles of the template regexes and then use that precompiled data in the performance tuning.

Also, you can batch input and output in mini batches that will sometimes improve the sending and receiving rate.

`-ib {number of events in each batch}`

`-ob {number of events in each batch}`

## Q&A
### Why does my cmd printout have square brackets around every line?
When using the -o cmd each batch of events will be printed on one line, separated by , and with [] around the batch. 
This behaviour is to illustrate the batch mechanism and will not be present if you write to file with `-o file -ofn {filename}`.

### What is a good regex to use with the -i counter module?
A regex could be `"(\d+)$"` since $ denotes end-of-string.

Example:

`java -jar LogGenerator-with-dependencies.jar -i counter -string "Test:" --limit 100000 -o null -s true -gd "(\d+)$"`

### This sounds nice and all, but how do I start? Can I get the built jar?
I won't be uploading a jar file, but you can easily get the jar by:

`git clone https://github.com/anders-wartoft/LogGenerator.git`

`cd LogGenerator`

`mvn clean package`

The jar is now in a directory called `target`.

Note that the package is developed with Java 11.

### I want to enable logging, how do I do that?
Logging can be enabled by adding instructions to the logging framework. The amount of logging is not great though.

Start the program as usual, but add `-Djava.util.logging.config.file=logging.properties` to java. 

Example: `java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -o cmd -ofn src/test/data/test-result.txt -i udp -ih localhost -ip 9999 -s true -g "**.txt" -e 100000`

### How do I send and receive from Kafka?
Example of sending a few lines to Kafka:

Server:

`java -Djava.util.logging.config.file=logging.properties -jar LogGenerator-with-dependencies.jar -o cmd -i kafka -icn testclient -itn test -ibs 192.168.1.116:9092  -gd "Test:(\d+)$" -s true  -rg true`

Client:

`java -jar target/LogGenerator-with-dependencies.jar -o kafka -ocn test2 -otn test -obs 192.168.1.116:9092 -i counter -string "Test:" --limit 100  -s true -ob 10`

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

If you are only interested in the _source, set `elastic-input-field=_source`. The result will now become:
``` 
[{"@timestamp":"2023-09-29T19:08:52","message":"Test row 11"}]
```
Note that the _id is not in the returned content here. For further selection, use a RegexFilter or a (future) JSONFilter for processing.
If the size of the data is large, you might want to consider just getting the _id with the _source parameter to minimize the impact on the cluster.

``` json
{"query": { "query_string": { "query": "*" }}, "_source": ["_id"]}
```
and in the property file:
`elastic-input-field=_source`
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
On the sending side, use the template file input `-i template -t file` or similar (see above).

Example: to send random data from a template file to an udp listener on port 9999 for 5 seconds, use:

`java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -i template -ifn src/test/data/template.txt -o udp -oh localhost -op 9999 -t time:5000 `

### The -e eps throttling doesn't work
Add `-s true` so that timestamps are updated.

Example: 

`java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -i template -ifn src/test/data/template.txt -o udp -oh localhost -op 9999 -t continuous --limit 100  -e 2 -s true`

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
-gd "<(\\d+)>"
```
Also, you can omit the escape:
``` basj
-gd "<(\d+)>"
```
But, in the property file you have to write the regex as is, without extra escape characters, like
```properties
gap-detection=<(\d+)>
```
There's an example in ssl-server.properties.

### What happens if I use a name for the standard variables for my custom variable
If there is a standard variable with a name of, e.g., `ip`, and you define a new variable with the same name, your variable definition will overwrite the standard variable.

## What License are you using?
See the license header in each java file. As long as you don't violate the licenses of the components (kafka and slf4j), you can do whatever you want with the code, just give me credit if you use the code.

