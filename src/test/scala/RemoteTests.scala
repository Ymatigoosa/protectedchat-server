import org.scalatest.matchers.MustMatchers
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.ByteString
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource
import java.io.PrintStream
import scala.concurrent._
/*class RemoteTests(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with WordSpec
  with MustMatchers
  with BeforeAndAfterAll
  //with DB
  //with UserQueries
  {

  def this() = this(ActorSystem("SocketHandlerSpec"))

  //override implicit def dispatcher = ExecutionContext.Implicits.global

  //val tsystem = ActorSystem("server")
  //val UserDB = tsystem.actorOf(UserDBProps.props(), "UserDBActor")
  //val service = tsystem.actorOf(TcpServer.props(SocketHandlerProps), "ServerActor")

  val ts = new Socket(InetAddress.getByName("54.225.172.112"), 8080)
  val in = new BufferedSource(ts.getInputStream()).getLines()
  val out = new PrintStream(ts.getOutputStream())

  val ts2 = new Socket(InetAddress.getByName("54.225.172.112"), 8080)
  val in2 = new BufferedSource(ts2.getInputStream()).getLines()
  val out2 = new PrintStream(ts2.getOutputStream())

  override def beforeAll {
    /*val a = for {
      _ <- execute("DELETE FROM friends")
      _ <- execute("DELETE FROM users")
      _ <- Query.addUser("abc","abc")
      _ <- Query.addUser("bcd","bcd")
      _ <- Query.addUser("ggg","ggg")
      _ <- Query.addUser("bbb","bbb")
      _ <- Query.makeFriends("abc","bbb")
      _ <- Query.makeFriends("abc","ggg")
    } yield 1
    Await.result(a, Duration.Inf)*/
  }

  override def afterAll {
    /*execute("DELETE FROM friends")
    execute("DELETE FROM users")*/
    ts.close()
    ts2.close()
    //TestKit.shutdownActorSystem(tsystem)
    //TestKit.shutdownActorSystem(system)
  }

  var token = new String

  "A SocketHandler" must {
    "dont register if nick exists" in {
      out.println("registration abc sha2")
      out.flush()
      assert(in.next() == "error \"nickname already exists\"")
    }

    "register user" in {
      out.println("registration 213 213")
      out.flush()
      assert(in.next() == "registered 213")
    }

    "dont register user with bad nickname" in {
      out.println("registration {213 213")
      out.flush()
      assert(in.next() == "error \"command syntax error\"")
    }

    "authorise user" in {
      out.println("authorisation 213 213")
      out.flush()
      val trstr1 = in.next()
      val trstr1arr = trstr1.split(" ")
      token = trstr1arr(2)
      assert(trstr1 == "authorised 213 "+token)
    }

    "logout user if rejoin" in {
      out2.println("authorisation 213 213")
      out2.flush()
      val tras = in2.next()
      token = tras.split(" ")(2)
      assert(tras == "authorised 213 "+token)
    }

    "logout afted rejoin" in {
      out2.println("logout "+token)
      out2.flush()
      assert(in2.next() == "logouted 213")
    }

    "authorise parallel clients" in {
      out2.println("authorisation bbb bbb")
      out2.flush()
      val trstr = in2.next()
      val trstrarr = trstr.split(" ")
      assert(trstr == "authorised bbb "+trstrarr(2))
    }

    "work after delay" in {
      Thread.sleep(5000)
      out.println("logout "+token)
      out.flush()
      assert(in.next() == "error \"bad token "+token+"\"")
    }

    "dont get ip if not authorised" in {
      out.println("getip "+token+" 213")
      out.flush()
      assert(in.next() == "error \"bad token "+token+"\"")
    }

    "get tp" in {
      out.println("authorisation 213 213")
      out.flush()
      val trstr2 = in.next()
      val trstr2arr = trstr2.split(" ")
      token = trstr2arr(2)
      assert(trstr2 == "authorised 213 "+token)

      out.println("getip "+token+" 213")
      out.flush()
      assert("ip 213 /?\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+".r.pattern.matcher(in.next()).matches)
    }

    "add friend" in {
      out.println("addfriend "+token+" abc")
      out.flush()
      assert(in.next() == "friendadded 213 abc")
    }

    "get friend list" in {
      out.println("logout "+token)
      out.flush()
      assert(in.next() == "logouted 213")

      out.println("authorisation abc abc")
      out.flush()
      val trstr3 = in.next()
      val trstr3arr = trstr3.split(" ")

      token = trstr3arr(2)
      out.println("getfriends "+token)
      out.flush()
      assert(in.next()=="friendlist abc (bbb,true),(ggg,false)")
    }

    /*"dont reuse actor names" in {
      out.close()
      ts.close()

      val ts3 = new Socket(InetAddress.getByName("localhost"), 9999)
      val in3 = new BufferedSource(ts3.getInputStream()).getLines()
      val out3 = new PrintStream(ts3.getOutputStream())

      out3.println("authorisation abc abc")
      out3.flush()
      //assert(in3.next() == "logouted 213")
    }*/
  }
}*/