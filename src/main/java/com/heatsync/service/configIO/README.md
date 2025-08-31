# Description

Component responsible from reading / writing to configuration files

For this documentation consider the following on the config file:
- On a map key : value, the key is called "operator" that contains the value


Divided into three sections:

## IO layer (ConfigFileIO.java + ConfigIOException,java):
- Handles reading / writing a provided config file with an expected file format
- Interfaces with other layer using PairList of Strings, which represents matching operator and string value
- The specific file format and conversion into data is handled by ConfigFileIO.java, however it could also be replaced by other libraries, as long as a conversion to the PairList is done, which is expected from the layer above

## Strict Checking Layer (FanProfileConfigIO.java)
Responsible for checking if the config file contains:
- Only the expected operators
- Values that correspond to the type and other restrictions imposed to values for the operator
- Prepare a container to deliver seamlessly and with type safety the matching values

Missing operators : value pairs do not trigger error. Instead they are allowed and assigned as null values
The layer above can determine how to handle the missing values

## Service Layer (FanProfileIOService.java)
Used by the other services. Handles:
- Definition of config absolute path on system
- Defition of default values for when the program first starts (config file does not exist), or for filling empty values
- Holding a global static field to provide free read access to the config fields obtained from the previous layer
- Initiation of reading / write / update operations on config file
- Handling how to handle missing operators fields

Default handling modes:
If keepStateFlag is set to true, missing fields of the config file will not be updated with defaults; Instead program warns about the errors and exit
If keepStateFlag is set to false, no exceptions are received

This only occurs during initialization, when fields are being resolved. Further blocks to writing operations will simply provide warns

