`app.ini` is made to hold two pieces of information:

* `install` to hold which library the ILE component is installed in
* `remote` to determine what system to connect to. This is primarily intended for development purposes, when the Distributor is running in a different environment than the Handler (e.g. your local Windows system). This information is not used when running on IBM i.

```ini
[install]
library=MANZAN

[remote]
system=youribmi
user=manzanuser
password=yoyoyo
```