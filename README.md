# MiniJava Compiler

This project provides a browser-based compiler for MiniJava, a subset of Java. It performs syntactic analysis, AST construction, identification, type checking, mJAM code generation, and assembly generation without executing submitted programs.

## Architecture

The web request path is:

`Browser -> Tomcat 9 -> CompilerServlet -> MiniJava compiler -> mJAM generator`

The project remains a JSP/Servlet application. Maven builds the existing `src` and `WebContent` Eclipse project layout into a standard WAR.

Each compilation receives a unique directory under the Java temporary directory. Source, compiler output, object code, and assembly are read during that request and then deleted. The latest assembly is held as bytes in the user's HTTP session for `/mjam`; no compilation requires persistent filesystem storage.

The compiler and mJAM generator contain process-wide static code-generation state. `CompilerService` therefore serializes complete compiler invocations with an application-level lock. Cloud Run concurrency can safely be raised from a compiler-correctness perspective, but compilation work within one container remains serialized; concurrency `1` is recommended initially while production resource use is measured.

Assembly downloads use container-local HTTP sessions. Keep the initial Cloud Run service at one maximum instance so a compile followed by `/mjam` reaches the same container. Supporting multiple instances while preserving this two-request workflow would require a different download design or external session storage and is outside this migration.

## Local Maven Build

Requirements: JDK 8 or newer and Maven 3.9 or newer.

```sh
mvn clean package
```

The reproducible artifact is `target/miniJava-compiler.war`.

## Local Docker

```sh
docker build -t minijava-compiler .
docker run --rm -p 8080:8080 minijava-compiler
```

Open <http://localhost:8080>. The health endpoint is <http://localhost:8080/health>.

To test a running container:

```sh
scripts/smoke-test.sh http://localhost:8080
```

The smoke test covers the home page, health endpoint, valid and invalid compilation, session-backed assembly download, simultaneous compilations, and session isolation.

## Google Cloud Run

The following example uses Artifact Registry. Replace every angle-bracket placeholder; no project IDs or credentials are stored in this repository.

```sh
gcloud auth login
gcloud config set project <PROJECT_ID>
gcloud services enable artifactregistry.googleapis.com cloudbuild.googleapis.com run.googleapis.com

gcloud artifacts repositories create <REPOSITORY> \
  --repository-format=docker \
  --location=<REGION>

gcloud builds submit \
  --tag <REGION>-docker.pkg.dev/<PROJECT_ID>/<REPOSITORY>/minijava-compiler

gcloud run deploy minijava-compiler \
  --image <REGION>-docker.pkg.dev/<PROJECT_ID>/<REPOSITORY>/minijava-compiler \
  --region <REGION> \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --cpu 1 \
  --concurrency 1 \
  --timeout 60 \
  --max-instances 1
```

Cloud Run supplies HTTP traffic on port `8080`, which is Tomcat's default listener. The container listens on all interfaces. The service uses request-based billing by default and scales to zero unless minimum instances are configured.

After deployment, run the smoke tests against the service URL:

```sh
scripts/smoke-test.sh https://<CLOUD_RUN_SERVICE_URL>
```

Use Google IAM and short-lived local or CI authentication. Do not create or commit service-account JSON keys. A future deployment workflow should use GitHub OIDC with Google Workload Identity Federation.

## CI

GitHub Actions builds the Maven WAR, builds the production Docker image, starts it, and runs the smoke tests. It does not deploy or require Google Cloud credentials.

## Heroku Status

The former Heroku deployment and `webapp-runner` process are legacy. Their `Procfile` was removed after local container validation. Delete the external Heroku application only after the deployed Cloud Run service has been tested.

The historical committed `miniJava_Compiler_Website.war` was removed after its sources and behavior were compared with the repository and the Maven-built replacement passed the container smoke tests. New releases are built reproducibly from source with Maven.
