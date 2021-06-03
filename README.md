**HAS project can be used for active/standby application development**
- It can be used double unit application development and detect current status.
- Also can be used as single unit.
- After staring sync server it will try to connect mate unit and will detect current status
- Possible application status are:
  - Active
  - Standby  
  - Unknown (status not detected yet) 
- There is an api for detect current application status
  - SyncApi
    - waitForActive --> will wait until become active
    - amIActive --> return true if unit is active

**This is an active/standby application example project not product ready**

**Code example**

- Start sync server:

`SyncConfig syncConfig = new SyncConfig("127.0.0.1", "127.0.0.1", SyncConfig.DEFAULT_PORT , 1455);`  
`SyncServer syncServer = new SyncServer(syncConfig);`  
`syncServer.start();`

- SyncApi usage:

`SyncApi syncApi = new SyncApi(SyncConfig.DEFAULT_PORT);`  
`syncApi.amIActive();`  
`syncApi.waitForActive();`
