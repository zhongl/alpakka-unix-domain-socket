/*
 *  Copyright 2019 Zhong Lunfu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import sbt._

object Dependencies {

  object Version {
    val akka = "2.5.19"
    val http = "10.1.6"
  }

  val common = Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "org.scalamock" %% "scalamock" % "4.1.0" % Test
  )

  val akka = Seq(
    "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % "1.0-M2",
    "com.typesafe.akka"  %% "akka-http"                              % Version.http,
    "com.typesafe.akka"  %% "akka-http-spray-json"                   % Version.http,
    "com.typesafe.akka"  %% "akka-http-testkit"                      % Version.http % Test
  )

}
