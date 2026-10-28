# Greatest of Three: Jenkins and Kubernetes

This example builds a small Java HTTP service, packages it in Docker, and deploys
it to Kubernetes from Jenkins. The service returns the greatest of three
integers: `GET /greatest?a=3&b=9&c=5` returns `9`. Its `/health` endpoint is
used by Kubernetes readiness and liveness probes.

## Run locally in VS Code

Install JDK 17 and Maven, open this folder in VS Code, and run:

```powershell
mvn -B clean verify
java -jar target/greatest-three-1.0.0.jar
```

In another terminal, try:

```powershell
curl.exe "http://localhost:8080/greatest?a=3&b=9&c=5"
```

The expected response is `9`.

## Jenkins setup

1. Make this project available in a Git repository and create a Jenkins
   **Pipeline** job configured to use its `Jenkinsfile` (Pipeline script from
   SCM).
2. Ensure the Jenkins Windows agent has JDK 17, Maven, Docker, PowerShell,
   `kubectl`, and network access to the Docker registry and Kubernetes API.
3. Have a cluster administrator create the namespace once:

   ```sh
   kubectl create namespace greatest-three
   ```

4. In **Manage Jenkins → Credentials**, add:
   - A Docker Hub username/password credential with ID `dockerhub-creds`.
   - A Secret file credential containing the cluster's kubeconfig with ID
     `kubeconfig`. Give its Kubernetes identity permission to manage resources
     in the `greatest-three` namespace.
5. Edit `IMAGE_REPO` near the top of `Jenkinsfile`, replacing
   `REPLACE_WITH_DOCKERHUB_USERNAME` with your Docker Hub username. If using a
   different registry, update the image path and the Docker login command for
   that registry.
6. Save the job and select **Build Now**. The stages check out the source,
   run Maven tests, build and push an image tagged with the Jenkins build
   number, apply the Kubernetes manifests, and wait up to three minutes for
   the Deployment rollout.
7. To check the running service from a machine with cluster access, run:

   ```sh
   kubectl port-forward service/greatest-three 8080:80 -n greatest-three
   ```

   Then request `http://localhost:8080/greatest?a=3&b=9&c=5`.

On rollout failure, the pipeline fails and prints the Deployment description,
matching Pods and their descriptions, recent namespace events, and the last
100 lines of container logs in the Jenkins console. These diagnostics help
identify issues such as image pull failures, scheduling/resource problems, and
application startup or probe failures.
