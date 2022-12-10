# LogGenerator
LogGenerator is a tool to debug log streams, i.e., syslog, Kafka, UDP diodes and similar chains of log collection systems.
The tool reads input from an input module, filters the input (add a header, replace text etc.) and finally writes the output with an output module.

### Input modules:
- Read files
- Read files in a directory (with glob)
- Receive UDP
- Receive TCP
- Fetch from Kafka topics

### Filter modules:
- Add a header
- Replace by regex
- Replace variables

### Output modules
- Write to file
- Write to console
- Write to UDP
- Write to TCP
- Write to Kafka


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

Example: `{pri:}` migh be substituted by `165`


## Order of substitution
The order of substitution is the following:
1. lorem
2. oneOf
3. random
4. date
5. ipv4
6. string
7. counter
8. pri

The substitutions can in some cases be chained.

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

`Sat Dec 10 00:35:57.108 Usb Host Notification Apple80211Set: seqNum 5459 Total 1 chg 0 en0`

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


== Chaining LogGenerator
Now that we have an understanding of the basics, we can progress to the more advanced use cases for LogGenerator.
To locate bottlenecks or bad connections in a log chain, one or more components can be substituted with LogGenerator to create a known set of data to send and receive.


== Q&A
=== Why does my cmd printout have square brackets around every line?
When using the -o cmd each batch of events will be printed on one line, separated by , and with [] around the batch. 
This behaviour is to illustrate the batch mechanism and will not be present if you write to file with `-o file -on {filename}`.

