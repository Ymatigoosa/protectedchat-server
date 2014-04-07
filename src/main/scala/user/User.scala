package user

/**
 * Created by Pahomov Dmitry on 03.03.14.
 */

object User {
  case class UserInfo(description: String)
}

import User.UserInfo
case class User(nickname: String, pwhash: String, info: UserInfo)
