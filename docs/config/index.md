
### Locations

Configuration files (`.ini` extension) are in different locations depending on where you setup Manzan.

* If running on IBM i, the configuration files can be found in `/QOpenSys/etc/manzan/`
* If running on a non-IBM i system, such as Mac or Windows, the configuration is in the working directory of where Manzan was started.

### Different files

Three configuration files exist:

* [`app.ini`](/config/app.md) for general Manzan configuration.
* [`data.ini`](/config/data.md) to configure different data sources
* [`dests.ini`](/config/dests.md) for configuring different destinations for the data to end upd