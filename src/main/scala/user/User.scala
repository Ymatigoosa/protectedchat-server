package user

/**
 * Created by 7 on 03.03.14.
 */

object User {
  case class UserInfo(description: String)
}

import User.UserInfo
case class User(nickname: String, pwhash: String, info: UserInfo)
