## Messaging

### Messaging Preference

By default, Manzan will send messages between the Handler component and the Camel component via socket communication. If however socket communication happens to fail for any reason, it will use SQL based communication as a fallback option. If instead, you prefer Manzan to use SQL based communication as a first option, you can set the environment variable `MANZAN_MESSAGING_PREFERENCE=SQL`. Set the environment variable back to the value `SOCKETS` to set your preference to socket communication.

### Messaging options
Manzan supports two options for sending messages between the Handler and the Camel component (SQL and socket communication).\
**SOCKET COMMUNICATION:** This option provides real time communication and is faster than the sql option.\
**SQL COMMUNICATION:** Using this option works via the Handler component inserting data into a Db2 table, and then the Camel component subsequently reading from the table. This option isn't quite as fast as socket communication, however the data is more durable in the case that the Camel component is malfunctioning. 

Socket communication is recommended, because by providing a fallback option to SQL in the case of socket communication malfunctioning, we guarantee data will not be lost.

* `MANZAN_MESSAGING_PREFERENCE = SQL`: Prefer SQL based communication
* `MANZAN_MESSAGING_PREFERENCE != SQL`: Prefer socket based communication

### Messaging port
By default, Manzan will try to use port 8080 for socket communication, but this can be changed by setting the environment variable `MANZAN_SOCKET_PORT`. Ex. 
```cl
ADDENVVAR ENVVAR(MANZAN_SOCKET_PORT) VALUE(8096) LEVEL(*SYS) REPLACE(*YES)
```


### Setting the MANZAN_MESSAGING_PREFERENCE

The `MANZAN_MESSAGING_PREFERENCE` environment variable can be set with the `ADDENVVAR` command. For example, to set the messaging preference to prefer SQL based communication run the command:

```cl
ADDENVVAR ENVVAR(MANZAN_MESSAGING_PREFERENCE) VALUE(SQL) LEVEL(*SYS) REPLACE(*YES)
```

After which you will need to run `ENDTCPSVR *SSHD` and `STRTCPSVR *SSHD`. Then restart your SSH session for the new environment variable to take effect.