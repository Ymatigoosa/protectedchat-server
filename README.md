protectedchat-server
====================

* I am a server for [Protected Video Chat](https://bitbucket.org/gvterechov/protected-video-chat).
* I am a academic course project of Volgograd State Tehnical University.
* I provide http json api for this app.
* I am writed on scala+akka+spray+sprayjson.
* I can do patten matching against json (like agaist regexp in scala). You can see it in [JsonPattern.scala](https://github.com/pahomovda/protectedchat-server/blob/master/src/main/scala/util/JsonPattern.scala)
* U can see me at POST http://guarded-citadel-75.herokuapp.com/api/json/

Server API
====================

Requests
--------------------
```
{
  "mode": "registration",
  "nickname": "?",
  "pw": "?"
}

{
  "mode": "authorisation",
  "nickname": "?",
  "pw": "?",
  "p2pip": "?"
}

{
  "mode": "logout",
  "token": "?"
}

{
  "mode": "getip",
  "token": "?",
  "nickname": "?"
}

{
  "mode": "addfriend",
  "token": "?",
  "nickname": "?"
}

{
  "mode": "removefriend",
  "token": "?",
  "nickname": "?"
}

{
  "mode": "getfriends",
  "token": "?"
}

{
  "mode": "update",
  "token": "?"
}
```

Replies
--------------------

```
{
  "status": "registered",
  "nickname": "?"
}

{
  "status": "authorised",
  "nickname": "?",
  "token": "?"
}

{
  "status": "logouted",
  "nickname": "?"
}

{
  "status": "ip",
  "nickname": "?",
  "ip": "?"
}

{
  "status": "friendadded",
  "owner": "?",
  "friend": "?"
}

{
  "status": "friendadded",
  "nickname": "?",
  "list": "?"
}

{
  "status": "updated",
  "nickname": "?"
}

{
  "status": "error", // You can check http response code when error for additional info
  "errormsg": "?"    // for example code 400 - Bad request
}
```
