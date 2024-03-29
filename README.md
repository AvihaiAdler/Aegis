## Aegis

Aegis is a simple anti-spam bot written in Java using [Javacord](https://javacord.org/), with the aim to help protect your server from malicious link scams. Aegis only saves the bare minimum about your server. Currently the collected data is: the server id, the server name and server configuration properties such as: server prefix, urls one decides to block etc.

Aegis will also log (internally) any message it deletes for a limited amount of time.

Aegis _doesn't_ listens to private messages and _doesn't_ sends private messages. This means that Aegis will respond to any command in the same channel it was invoked. I **highly** suggest to create a hidden channel - visible only to the administrators of the server and use the commands there.

Upon joining a server Aegis will attempt to create a logging channel with the name `aegis-log`. I suggest to make that channel read only for everyone besides Aegis. Aegis will log every action it performs there. The channel can be moved to any category after its creation, just make sure Aegis can sees it and can writes to it.

### Modes

Aegis can operate in one of 2 modes:

- restricted: Aegis will read and delete every message which has a `@everyone` tag and includes a url.
- unrestricted: Aegis will delete messages which contains some words an admin deem as suspicious / contains urls an admin deem as blocked. This mode is the default mode for Aegis

### Functionality

Aegis has 3 different functionalities:

- Restrict mode (explained above)
- Aegis will scan messages for words an admin specified with the `suspect` command. If Aegis finds `threshold` or above number of those suspicious words in the message - it will delete the message and log the action
- Aegis will scan messages for urls you specified with the `block` command. If Aegis finds a blocked url in a message - it will delete it and log the action

### Commands

Aegis has a set of commands (listed below) one can use, note that currently only people with ADMINISTRATOR privileges / the server owner can invoke them.
All commands must start with a prefix (`!` by default) except for the first one on the list.

| Command    | Parameters                                   | Effect                                                                                          | Privileges  |
| ---------- | -------------------------------------------- | ----------------------------------------------------------------------------------------------- | ----------- |
| @Aegis     | None                                         | Aegis will display a short version of this table                                                | EVERYONE    |
| info       | None                                         | Aegis will display all the information it holds about your server                               | ADMIN/OWNER |
| logto      | Channel id                                   | Aegis will start to log into the channel you specified                                          | ADMIN/OWNER |
| prefix     | Character/s                                  | Aegis will replace the prefix for each command on your server                                   | ADMIN/OWNER |
| threshold  | An Integer between 0-(2^31)-1 (0 by default) | Aegis will set the threshold to the number you specified                                        | ADMIN/OWNER |
| restrict   | None                                         | Aegis will operate in restrict mode (see [above](https://github.com/AvihaiAdler/Aegis#modes))   | ADMIN/OWNER |
| unrestrict | None                                         | Aegis will operate in unrestrict mode (see [above](https://github.com/AvihaiAdler/Aegis#modes)) | ADMIN/OWNER |
| suspect    | A list of words separated by spaces          | Aegis will save the specified words in its suspicious list                                     | ADMIN/OWNER |
| unsuspect  | A list of words separated by spaces          | Aegis will removed the specified words from its suspicious list                                | ADMIN/OWNER |
| block      | A list of (valid) urls separated by spaces   | Aegis will save the specified urls in its blocked list                                         | ADMIN/OWNER |
| unblock    | A list of (valid) urls separated by spaces   | Aegis will remove the specified urls from its blocked list                                     | ADMIN/OWNER |

### Known issues

The `info` command doen't work properly when trying to 'switch' pages. Most of the time you require to react twice to get the next page

### Future development

- [x] The `info` command can benefit from Buttons instead of Reactions
- [ ] Turning Aegis into a full fledged moderation bot
- [ ] Migrating to slash commands
- [x] implement a simple counter measure against flood messages 'attacks'

### Deployment

This section is for further development of Aegis and doesn't aim to explain how to self host it.

- build with: `docker build -t image_name:image_tag .`
- tag with: `docker tag image_local_name:image_local_tag image_remote_name:image_remote_tag`
- push with: `docker push image_remote_name:image_remote_tag`
- create a `docker-compose` file (`.yaml`) with the following fields:

```yaml
version: "2"
services:
  aegis:
    image: repo_address
    ports:
      - 443:443
    environment:
      MONGO_CRED: mongo_connection_string
      TOKEN: token
    depends_on:
      - mongodb

  mongodb:
    image: mongo
    restart: always
    ports:
      - 27017:27017
    environment:
      MONGO_INITDB_ROOT_USERNAME: username
      MONGO_INITDB_ROOT_PASSWORD: password
```

- run with: `docker-compose -f file_name.yaml up -d`
