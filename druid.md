1 . Load druid with version 0.15.0

`curl http://apache.cp.if.ua/incubator/druid/0.15.0-incubating/apache-druid-0.15.0-incubating-bin.tar.gz -o apache-druid-0.15.0-incubating-bin.tar.gz`

2 . Unpack and rename folder to druid

`tar -xzf apache-druid-0.15.0-incubating-bin.tar.gz`
`mv apache-druid-0.15.0-incubating-bin.tar.gz druid`

3 . In druid the package root, run the following commands to load zookeeper:

`curl https://archive.apache.org/dist/zookeeper/zookeeper-3.4.11/zookeeper-3.4.11.tar.gz -o zookeeper-3.4.11.tar.gz
 tar -xzf zookeeper-3.4.11.tar.gz
 mv zookeeper-3.4.11 zk`
 
 4 . In the Druid package root, run the following commands to load tranquility:
 
 `curl http://static.druid.io/tranquility/releases/tranquility-distribution-0.8.3.tgz -o tranquility-distribution-0.8.3.tgz
  tar -xzf tranquility-distribution-0.8.3.tgz
  mv tranquility-distribution-0.8.3 tranquility`
  
  5 . Enable Tranquility Server
  
  In your conf/supervise/single-server/micro-quickstart.conf, uncomment the tranquility-server line.
  
  6 . From druid-integration module copy indicators-server.json file into 
  
  `<druid root>/conf/tranquility`
  
  7 . Change tranquility configuration to use new configuration file
  
  Go to `druid/conf/supervise/single-server` and modify `micro-quickstart.conf` file
  You need to change config file property to
  
  `-configFile conf/tranquility/indicators-server.json`
  
  8 . To start druid , run the following command from root
  
  `./bin/start-micro-quickstart`
  
  