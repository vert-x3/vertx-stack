# Install maven
wget http://wwwftp.ciril.fr/pub/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
tar -zxvf apache-maven-3.0.5-bin.tar.gz -C /usr/local && rm apache-maven-3.0.5-bin.tar.gz

# Clone and install the vertx-parent repository
git clone --depth 1 https://github.com/vert-x3/vertx-parent $GIT_BRANCH /home/work/vertx-parent
cd /home/work/vertx-parent && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-codegen repository
git clone --depth 1 https://github.com/vert-x3/vertx-codegen $GIT_BRANCH /home/work/vertx-codegen
cd /home/work/vertx-codegen && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-docgen repository
git clone --depth 1 https://github.com/vert-x3/vertx-codegen $GIT_BRANCH /home/work/vertx-docgen
cd /home/work/vertx-docgen && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vert.x repository
git clone --depth 1 https://github.com/eclipse/vert.x $GIT_BRANCH /home/work/vert.x
cd /home/work/vert.x && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true -Pdist install

# Clone and install the vertx-hazelcast repository
git clone --depth 1 https://github.com/eclipse/vertx-hazelcast $GIT_BRANCH /home/work/vertx-hazelcast
cd /home/work/vertx-hazelcast && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true -Pdist install

# Clone and install the vertx-codetrans repository
git clone --depth 1 https://github.com/vert-x3/vertx-codetrans $GIT_BRANCH /home/work/vertx-codetrans
cd /home/work/vertx-codetrans && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-service-factory repository
git clone --depth 1 https://github.com/vert-x3/vertx-service-factory $GIT_BRANCH /home/work/vertx-service-factory
cd /home/work/vertx-service-factory && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-maven-service-factory repository
git clone --depth 1 https://github.com/vert-x3/vertx-maven-service-factory $GIT_BRANCH /home/work/vertx-maven-service-factory
cd /home/work/vertx-maven-service-factory && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-service-proxy repository
git clone --depth 1 https://github.com/vert-x3/vertx-service-proxy $GIT_BRANCH /home/work/vertx-service-proxy
cd /home/work/vertx-service-proxy && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-lang-js repository
git clone --depth 1 https://github.com/vert-x3/vertx-lang-js $GIT_BRANCH /home/work/vertx-lang-js
cd /home/work/vertx-lang-js && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-lang-groovy repository
git clone --depth 1 https://github.com/vert-x3/vertx-lang-groovy $GIT_BRANCH /home/work/vertx-lang-groovy
cd /home/work/vertx-lang-groovy && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-ext-parent repository
git clone --depth 1 https://github.com/vert-x3/vertx-ext-parent $GIT_BRANCH /home/work/vertx-ext-parent
cd /home/work/vertx-ext-parent && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-routematcher repository
git clone --depth 1 https://github.com/vert-x3/vertx-routematcher $GIT_BRANCH /home/work/vertx-routematcher
cd /home/work/vertx-routematcher && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-reactive-streams repository
git clone --depth 1 https://github.com/vert-x3/vertx-reactive-streams $GIT_BRANCH /home/work/vertx-reactive-streams
cd /home/work/vertx-reactive-streams && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-sockjs repository
git clone --depth 1 https://github.com/vert-x3/vertx-sockjs $GIT_BRANCH /home/work/vertx-sockjs
cd /home/work/vertx-sockjs && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-embedded-mongo-db repository
git clone --depth 1 https://github.com/vert-x3/vertx-embedded-mongo-db $GIT_BRANCH /home/work/vertx-embedded-mongo-db
cd /home/work/vertx-embedded-mongo-db && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-mongo-service repository
git clone --depth 1 https://github.com/vert-x3/vertx-mongo-service $GIT_BRANCH /home/work/vertx-mongo-service
cd /home/work/vertx-mongo-service && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-metrics repository
git clone --depth 1 https://github.com/vert-x3/vertx-metrics $GIT_BRANCH /home/work/vertx-metrics
cd /home/work/vertx-metrics && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-rx repository
git clone --depth 1 https://github.com/vert-x3/vertx-rx $GIT_BRANCH /home/work/vertx-rx
cd /home/work/vertx-rx && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Clone and install the vertx-stack repository
git clone --depth 1 https://github.com/vert-x3/vertx-stack $GIT_BRANCH /home/work/vertx-stack
cd /home/work/vertx-stack && /usr/local/apache-maven-3.0.5/bin/mvn -DskipTests=true install

# Unar
tar -xvzf /home/work/vertx-stack/stack-dist/target/vert.x-3.0.0-SNAPSHOT.tar.gz -C /usr/local

# Cleanup
rm -rf /home/work /root/.m2 /usr/local/apache-maven-3.0.5