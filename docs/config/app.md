`app.ini` is made to hold two pieces of information:

* `install` to hold which library the ILE component is installed in
* `remote` to determine what system to connect to. This information is optional when running on IBM i and intended to be used when the Distributor is running in a different environment than the Handler. (e.g. your local Windows system)

```ini
[install]
library=MANZAN

[remote]
system=youribmi
user=manzanuser
password=yoyoyo
```