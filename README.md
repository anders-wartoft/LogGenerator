# LogGenerator
LogGenerator is a tool to debug log streams, i.e., syslog, Kafka, UDP diodes and similar chains of log collection systems.
The tool reads input from an input module, filters the input (add a header, replace text etc.) and finally writes the output with an output module.

### Input modules:
There are input module for the following tasks:
- Read files
- Read files in a directory
- Read files in a directory with globs
- Receive UDP
- Receive TCP 
- Fetch from Kafka topics
- Static string
- Static string ending with a counter starting from 1

#### Read files
Read a local file

Parameters: `-i file -in {file name}`

Example: `-i file -in ./src/test/data/test.txt`

#### Read files in a directory
Read all files in a directory

Parameters: `-i file -in {directory name}`

Example: `-i file -in ./src/test/data/`

#### Read files in a directory with globs
Read all files in a directory that matches a glob. See https://javapapers.com/java/glob-with-java-nio/ for details on how to write globs.

Parameters: `-i file -in {directory name -g {glob}`). 

Example: `-i file -in ./src/test/data/**.txt`

#### Receive UDP
Set up a UDP server. 

Parameters: `-i udp -in {address[:port number]}`

Example: `-i udp -in localhost:5999` or `-i udp -in 5999`

#### Receive TCP
Set up a TCP server. 

Parameters: `-i tcp -in {address[:port number]}`

Example: `-i tcp -in 192.168.1.2:5999` or `-i tcp -in 5999`

#### Fetch from Kafka topics
Connect to a Kafka server and read from a topic

Parameters: `-i kafka -cn {client name} -tn {topic name} -bs {boostrap server}`

Example: `-i kafka -cn test -tn testtopic -bs localhost:9092`

#### Static string
Send the same string over and over again. 

Parameters: `-i static -in {the string to send}`

Example: `-i static -in "Test string to send"`

Example: `-i static -in "Test string to send" -l 1000` (only send 1000 events)

#### Static string ending with a counter starting from 1
This is used to send a static string that is appended with an increasing number, starting from 1. This is a very fast way to send events ending with a counter.
To send 1.000.000 events from "Test:1" to "Test:1000000", use `java -jar LogGenerator-with-dependencies.jar -i counter -in "Test:" -limit 1000000 -o cmd`

Parameters: `-i counter -in {the string to send}`

Example: `-i counter -in "Test string number:"`


### Output modules
These are the output modules available:
- Write to file
- Write to console
- Write to UDP
- Write to TCP
- Write to Kafka
- Write to null (throw away the result, used for performance testing)

#### Write to file
Write the received events to a local file.

Parameters: `-o file -on {file name}`

Example: `-o file -on ./received.txt`

#### Write to console
Write the received events to the console. 

Parameters: `-o cmd`

Example: `-o cmd`

#### Write to UDP
Send events with UDP.

Parameters: `-o udp -on {host:port}`

Example `-o udp -on localhost:5999}`

#### Write to TCP
Send events with TCP.

Parameters: `-o tcp -on {host:port}`

Example `-o tcp -on localhost:5999}`

#### Write to Kafka
Connect to a Kafka server and write the events to a topic. N.B., these are not unique arguments for kafka input and output so there is no way to read from a Kafka topic and write to another topic. The tool is not meant to be used for that kind of usage.

Parameters: `-o kafka -cm {client name} -tn {topic name} -bs {boostrap server}`

Example: `-o kafka -cn test -tn testtopic -bs localhost:9092`

#### Write to null
This will throw away the result. It is useful, e.g., when testing for performance.

Parameters: `-o null`

Example: `-o null`

To send the text "test" 100.000 times over UDP and discard the result, but to see the eps and bps, use:

Server:

`java -jar LogGenerator-with-dependencies.jar -i udp -in 9999  -o cmd  -s true  -gd "(\\d+)$"`

In another command window, start the client (same jar file):

`java -jar LogGenerator-with-dependencies.jar -i counter  -in "test" -o udp -on localhost:9999  -s true -limit 1000`

### Filter modules:
- Add a header
- Replace by regex
- Replace variables
- Detect gaps

#### Add a header
To send a file line by line but to each line prepend a header, that can contain text and variables.

Parameters: `-he {header text}`

Example: `-he {syslog-header}`, `-he "My custom header with date: {date:yyyyMMdd}: "`

#### Replace by regex
A use case is if you have a lot of nice logs, but the date is not possible to use. You can load the file and add a regex to find the date, then replace the date with a {date:...} variable or a static string.
There must be a capture group in the regex. The text matched by the capture group will be replaced by the value.

Parameters: `-r {regex to find} -v {value to insert instead of the part matched by the regex}`

Example: `-r "<(\\d+)>" -v "<2>"`

If you have a file with a lot of logs, like:

`[Sat Dec 03 00:35:57.399 Usb Host Notification Apple80211Set: seqNum 5460 Total 1 chg 0 en0]`

Then the following invocation will change the date to today:

`java -jar LogGenerator-with-dependencies.jar -i file -in src/test/data/log-with-time.log -o cmd -s true -l 10 -r "([a-zA-Z]{3} [a-zA-Z]{3} \d\d \d\d:\d\d:\d\d\.\d{3})" -v "{date:EEE MMM HH:mm:ss.sss}"`

#### Replace variables
Variable substitution will be present for template, regex and header processing. If a file is loaded as "file" or template "none" then the (processor intensive) substitutions will not be loaded.

#### Detect gaps
Gaps are a continuous block of missing numbers. We use gaps to inform the filter that we are missing some events.
The GapDetector will inspect each event, search for an identification number by a regex and then check if the event has been seen before.
If an event is missing then this will be reported after the end of processing.

Parameters `-gd {regex with caputure group}`

Example: `-gd "<(\d+)>"`

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

#### Ipv4
Insert an ip address from a specified subnet.

Syntax: `{ipv4:(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})/(\d{1,2})}`

Example: 
- `{ipv4:192.168.1.182/24}` might be resolved to `192.168.1.14`

#### OneOf
This variable will randomly pick one of the supplied arguments.

Syntax: `{oneOf:(?<options>.*)(/(?<delimiter>.))?}`

Example:
- `{oneOf:a,b,c,d}` will be replaced by one of a, b, c or d
- `{oneOf:a;b;c;d/;}` if , is needed in the options, a delimiter can be provided in the template.

#### Random
A Random variable will pick a random number in the specified interval (included).

Syntax: `{random:(?<from>\d+)-(?<to>\d+)}`

Example: `{random:1-6}` will be evaluated to 1, 2, 3, 4, 5 or 6.

#### String
A string made from a number of characters. The characters to choose from and the length of the string is specified.

Syntax: `{string:(?<characters>[^/]+)/(?<numberof>\d+)}`

Example: `{string:a-c/8`} will for example create `aaaccaca`

The first argument can be for example a-zA-Z0-9_\- to include letters, numbers, the underscore _ and the hyphen - characters.

#### Lorem.
The Lorem variable will pick a number of random word from a list of words and add with a delimiter between.

Syntax: `{lorem:(?<length>\d+):(?<wordlist>.*)/(?<delimiter>.)}`

Example: `{lorem:4:this is a nice story of random words that will be assembled but just four of the words/ }` 
can for example be substituted with `words nice words is`.

#### Counter
A counter is an integer with a starting value that will be substituted with the value of the counter on the first invocation. 
On subsequent invocations, the value is increased by 1 for each invocation.

To differentiate between counters each counter has its own unique name. 

Syntax: `{counter:(?<name>[a-zA-Z0-9\-_]+):(?<startvalue>\d+)}`

Example: `{counter:counterName:14}` will be substituted by `14` the first time the template is evaluated, `15` the next time and so on.

#### Pri
A pri will just create a random facility, severity and priority for a syslog message and
return the priority value that is used in the syslog header.

Syntax: `{pri:}`

Example: `{pri:}` might be substituted by `165`


## Order of substitution
The substitutions will process one after another and repeat until the result is stable, so the output of one filter can be the input to another.

Example: `{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}` will evaluate to a random RFC 1918 address.

## Usage
### Read a file and print the output to the console

`java -jar LogGenerator-with-dependencies.jar -i file -in file.txt -o cmd`

### Read a file and send the output to a UDP listener

`java -jar LogGenerator-with-dependencies.jar -i file -in file.txt -o udp -on localhost:514`

### Read a file, add a syslog header and send the output to the console
This will add a syslog header to each line in the file before printing the line.

`java -jar LogGenerator-with-dependencies.jar -i file -in test.txt -o cmd --he "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ""`

Example: 
- `<25>Dec 10 15:27:38 192.168.169.209 liiblhukp[38946]: Test row 1`
- `<165>Dec 10 15:27:38 localhost 0th3ih5rq[13347]: Test row 2`

### Read a file, change the date from the lines in the file to the date right now and print to the console

Example: If a log locks like this: 

`Sat Dec 03 00:35:57.108 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0`

`java -jar LogGenerator-with-dependencies.jar -i file -in log-with-time.log -r "[a-zA-Z]{3} [a-zA-Z]{3} \d{1,2} \d\d:\d\d:\d\d\.\d{3}" -v "{date:EEE MMM dd HH:mm:ss}" -o cmd`

might print 

`Sat Dec 10 16:02:24 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0` (note the date and time change)

## Templates
So far, we have only used variables in headers, but we can also use variables in the files. The template engine will only load if one of these conditions are met:

- The regex parameter is set on the command line
- The header parameter is set on the command line
- The template is set to something other than 'none'

The templates use regexes so if we can avoid to load and run them on each entry we can save a lot of processing power. 
For applications that need lots of eps, try generating the events to file first and then use LogGenerator or other application to load from disk.

### Template files
A template file is just a file with one or more lines that contain text and/or variables ({date: ...). Templates can be run as:

- none: meaning the file will just be processed as usual but in a random order (if not regex or header is used at the same time)
- file: read one random line from the file and send to the filter output. The line will not be read again. When the last line has been sent, the program halts.
- continuous: read one random line from the file and send to the filter output. The line might be sent again.

Example. We have a template file with two lines:

- `row1 {oneOf:a,b,c,d}`
- `row2 {random:10-13}`

The result from running: 

`java -jar LogGenerator-with-dependencies.jar -i template -t continuous -in template.txt -o cmd  -s true -l 10000`

will be 10.000 lines beginning with `row1` or `row2` and ending with a letter a-d or number 10-13.

With a syslog template, a great number of unique logs can be generated.

### System variables
Some variables are built-in, expanding to other variables and thus easier to use.
To use the syslog-header built-in variable, add `{syslog-header}` to either the template file or the header command line argument. 

The following system variables can be used:

- syslog-header: `<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: `
- ip: `{<ipv4:0.0.0.0/0}`
- rfc1918: `{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}`

## Chaining LogGenerator
Now that we have an understanding of the basics, we can progress to the more advanced use cases for LogGenerator.
To locate bottlenecks or bad connections in a log chain, one or more components can be substituted with LogGenerator to create a known set of data to send and receive.

LogGenerator can be used to generate logs and receive logs, count them, calculate events per second and even detect lost events.

### Lost events - Gap detection
If the events transferred contains an increasing value, then the receiver can detect if any of the values are missing.

To generate a header with that kind of value, use the {counter: variable.

`{counter:name:1}`

Example: Send data from a file, add a header with a counter and some text around the counter, so we can identify that on the server side.
First, send to cmd, so we can see that the counter is working:

`java -jar LogGenerator-with-dependencies.jar -i file -in src/test/data/log-with-time.log -he "<{counter:test:1}>" -o cmd`

We should se some events starting with <1>, <2> etc. Since we'd like to be able to see if the Gap Detection is working, 
set the initial value to, e.g., 42.

Example with only one instance of LogGenerator:

Server and client:

`java -jar LogGenerator-with-dependencies.jar -i file -in src/test/data/test.txt -he "<{counter:test:42}>" -o cmd -gd "<(\\d+)>"`

You should see the received data (probably on one line) and the detected gaps:

`1-41`

Example with one client and one server:

In a new terminal, start the server:
`java -jar LogGenerator-with-dependencies.jar -i tcp -in 9999 -o cmd  -gd "<(\\d+)>"`

Start the client:

`java -jar LogGenerator-with-dependencies.jar -i file -in src/test/data/test.txt -he "<{counter:test:42}>" -o tcp -on localhost:9999`

You should see the received data. To see the gaps, press Ctrl-C:
`Next expected number: 48`

`1-41`

Now it's easy to configure the first LogGenerator to send logs to, e.g., Kafka or rsyslog and connect the LogGenerator receiver to accept UDP connections or read from Kafka.
By sending more data and possibly throttle the data, you can test how much load different parts of the log chain can handle.

## Improving performance
First, regexes can be really slow. If you want good performance, use the static or counter input module and no headers. 
A good way to improve performance is to create a lot of events in a file or Kafka, with all the bells and whistles of the template regexes and then use that precompiled data in the performance tuning.

Also, you can batch input and output in mini batches that will sometimes improve the sending and receiving rate.

`-ib {number of events in each batch}`

`-ob {number of events in each batch}`

## Q&A
### Why does my cmd printout have square brackets around every line?
When using the -o cmd each batch of events will be printed on one line, separated by , and with [] around the batch. 
This behaviour is to illustrate the batch mechanism and will not be present if you write to file with `-o file -on {filename}`.

### What is a good regex to use with the -i counter module?
A regex could be `"(\d+)$"` since $ denotes end-of-string.

Example:

`java -jar LogGenerator-with-dependencies.jar -i counter -in "Test:" -limit 100000 -o null -s true -gd "(\d+)$"`

### This sounds nice and all, but how do I start? Can I get the built jar?
I won't be uploading a jar file, but you can easily get the jar by:

`git clone https://github.com/anders-wartoft/LogGenerator.git`

`cd LogGenerator`

`mvn clean package`

The jar is now in a directory called `target`.

Note that the package is developed with Java 17. It might work on earlier releases but that is not a goal with the project.

### I want to enable logging, how do I do that?
Logging can be enabled by adding instructions to the logging framework. The amount of logging is not great though.

Start the program as usual, but add `-Djava.util.logging.config.file=logging.properties` to java. 

Example: `java -Djava.util.logging.config.file=logging.properties -jar target/LogGenerator-with-dependencies.jar -o cmd -on src/test/data/test-result.txt -i udp -in localhost:9999 -s true -g "**.txt" -e 100000`

### How do I send and receive from Kafka?
Example of sending a few lines to Kafka:

Server:

`java -Djava.util.logging.config.file=logging.properties -jar LogGenerator-with-dependencies.jar -o cmd -i kafka -cn testclient -tn test -bs 192.168.1.116:9092  -gd "Test:(\d+)$" -s true  -rg true`

Client:

`java -jar target/LogGenerator-with-dependencies.jar -o kafka -cn test2 -tn test -bs 192.168.1.116:9092 -i counter -in "Test:" -limit 100  -s true -ob 10`

