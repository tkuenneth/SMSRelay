### Welcome to SMS Relay

The purpose of this small app is to forward notifications about missed calls, as well as incoming SMS/MMS messages via email.

To build the app, you need to add a few lines to *local.properties*:

```
SMTP_HOST=smtp.example.com
SMTP_PORT=465
SMTP_USERNAME=email@example.com
SMTP_PASSWORD=<the password>
SMTP_FROM=sender@example.com
SMTP_TO=receiver@example.com
```
