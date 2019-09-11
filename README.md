# dicom-storescp
A simple DICOM Storage Service Class Provider (SCP)

Run with gradle:
> ./gradlew bootRun

Or, run in a container:
> ./gradlew build && docker build -t storescp . && docker run -P --name testscp storescp 
