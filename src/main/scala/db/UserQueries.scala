package db

/**
 * Created by 7 on 06.04.2014.
 */
trait UserQueries {
  this: DB =>
  object Query {
    def addUser(nickname: String, pwhash: String) = execute("INSERT INTO users(nickname, pwhash, userinfo) VALUES (?, ?, ?)",nickname, pwhash, "")
    def getUser(nickname: String, pwhash: String) = execute("SELECT id FROM users WHERE nickname=? AND pwhash=?", nickname, pwhash)
    def makeFriends(owner: String, friend: String) = execute("INSERT INTO friends(owner, friend) SELECT u1.id, u2.id FROM users AS u1, users AS u2 WHERE u1.nickname=? AND u2.nickname=? AND NOT EXISTS (SELECT owner, friend FROM friends WHERE owner = u1.id AND friend=u2.id)", owner, friend)
    def unfriend(owner: String, friend: String) = execute("DELETE FROM friends USING friends, users AS u1, users AS u2 WHERE u1.nickname=? AND owner=u1.id AND u2.nickname=? AND friend=u2.id", owner, friend)
    def getFriendList(nickname: String) = fetch("SELECT u2.nickname FROM users AS u1, users AS u2, friends WHERE u1.nickname=? AND u1.id=friends.owner AND u2.id=friends.friend", nickname)
  }
}
