### Expected behaviour
Tell us what should happen
Created picture(s) should get uploaded in a (sub)folder in my ownCloud 6 instance on my server.

### Actual behaviour
Tell us what happens instead
Since version 1.5.7 of ownCloud app I always get error message, that the task couldnt be done. "Hochladen fehlgeschlagen" is the original text on my Android display. ownCloud app tells "Hochladen <file> konnte nicht abgeschlossen werden".

### Steps to reproduce
1. Starting ownCloud app
2. Select desired destination folder
3. Select desired items on SD Card

### Environment data
Android version:
2.2

Device model: 
HTC Legend

Stock or customized system:


ownCloud app version:
1.5.7

ownCloud server version:
6.0.0

### Logs
#### Web server error log
```
Insert your webserver log here
```
Seems that there come no connect. I can't find any items from mobile Device's IP.

#### ownCloud log (data/owncloud.log)
```
Insert your ownCloud log here
```
{"app":"PHP","message":"Module 'pdo_mysql' already loaded at Unknown#0","level":3,"time":"2014-06-13T14:28:28+00:00"}
