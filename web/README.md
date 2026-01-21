# web

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/web-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)


## TODO

- [ ] Think how job updates happen:

use a multi?? with an manageded executor service that runs the job and sends updates to the client via SSE

- [ ] Job updates are sent to the client via SSE

```shell
  mvn package
  docker image build --build-arg KEY=c59c14ac-2d66-4da6-82c8-13b419016c1e --tag webanimo-uppaal-5.1.0 -f src/main/docker/Dockerfile.jvm .                                
  docker tag webanimo-uppaal-5.1.0 harbor.utsp.utwente.nl/library/webanimo-uppaal-5.1.0:latest
  docker image pull redis
  docker tag redis harbor.utsp.utwente.nl/library/redis-webanimo-custom:latest
  docker compose up -d
  
#  push to docker
  docker tag webanimo-uppaal-5.1.0 marien99/webanimo-backend:latest
  docker push marien99/webanimo-backend:latest
  
  # deploy on k8s
   kubectl create deployment web-animo-v2 --image=marien99/webanimo-backend:latest
  kubectl expose deployment quarkus --type=LoadBalancer --port=8080     
  
  # replace image
  kubectl set image deployments/kubernetes-bootcamp kubernetes-bootcamp=docker.io/jocatalin/kubernetes-bootcamp:v2

  kubectl apply -f .\kompose-output\kompose.yml
  
  # info
  kubectl config use-context docker-desktop
  kubectl config use-context cluster-k81y
  kubectl logs -f -l app=quarkus --all-containers=true  
   kubectl logs -f quarkus-6884dd45f4-8r882
  kubectl cluster-info dump
  
  # helm chart
  helm repo add bitnami https://charts.bitnami.com/bitnami
  helm install web-animo-frontend --generate-name
  
  helm install web-animo-backend --generate-name     
  
   helm upgrade web-animo-backend-1766478709 web-animo-backend 
   
    kubectl logs deployment/quarkus --all-pods=true
    kubectl get pods -n default -l app=quarkus -o wide
```

