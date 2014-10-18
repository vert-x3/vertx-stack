# Install maven
wget http://wwwftp.ciril.fr/pub/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
tar -zxvf apache-maven-3.0.5-bin.tar.gz -C /usr/local && rm apache-maven-3.0.5-bin.tar.gz

# Clone and install the codegen repository
git clone --depth 1 https://github.com/vert-x3/codegen $GIT_BRANCH /home/work/codegen
cd /home/work/codegen && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true  install

# Clone and install the vert.x repository
git clone --depth 1 https://github.com/eclipse/vert.x $GIT_BRANCH /home/work/vert.x
cd /home/work/vert.x && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true -Pdist install

# Clone and install the vertx-js repository
git clone --depth 1 https://github.com/vert-x3/vertx-js $GIT_BRANCH /home/work/vertx-js
cd /home/work/vertx-js && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-groovy repository
git clone --depth 1 https://github.com/vert-x3/vertx-groovy $GIT_BRANCH /home/work/vertx-groovy
cd /home/work/vertx-groovy && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-stack repository
git clone --depth 1 https://github.com/vert-x3/vertx-stack $GIT_BRANCH /home/work/vertx-stack
cd /home/work/vertx-stack && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install