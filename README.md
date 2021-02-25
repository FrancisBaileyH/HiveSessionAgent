### About
A *WIP* SMS chatbot that enables you to create alerts for climbing sessions at the Hive. This way there's no need to refresh the page periodically in the hopes that an opening appears. You will be notified within 10 minutes if an opening appears. It can also be used to set a reminder for future climbing sessions that aren't available for booking yet.


### Milestones
- Hive Schedule Polling [Done]
- Hive Session Notification [Partial]
  - Needs to use Pinpoint for two messaging. Works with SNS currently
- Hive Session SMS Alert Creation [In Progress]
- Hive Session SMS Allow List [Not Started]
- Hive Schedule 


### Setup
1. Install SAM
1. Create SAMUser IAM User with Admin permissions
1. Add AWS Credentials for SAM
1. Run:
```
./gradlew shadowJar
sam deploy --guided
```