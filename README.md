# CI/CD Pipeline using Jenkins, Docker, Trivy and Kubernetes

## Project Overview

This project demonstrates a **complete CI/CD pipeline** for deploying a Java application to a Kubernetes cluster using **Jenkins automation**.

The pipeline performs the following tasks automatically:

1. Pull source code from GitHub
2. Build the application using Maven
3. Create a Docker image
4. Scan the image for vulnerabilities using Trivy
5. Push the image to Docker Hub
6. Deploy the application to a Kubernetes cluster

This setup helps automate application delivery while ensuring security through vulnerability scanning.

---

# Repository Links

**GitHub Repository**

https://github.com/ashish200214/project_6_DevOps

**Docker Hub Repository**

https://hub.docker.com/repository/docker/ashish200214/project5

---

# Architecture

```
GitHub
   ↓
Jenkins CI Pipeline
   ↓
Maven Build
   ↓
Docker Image
   ↓
Trivy Security Scan
   ↓
Docker Hub
   ↓
Kubernetes Cluster
   ↓
Application Deployment
```

---

# Step 1 – Launch Jenkins Server (EC2)

Create an **EC2 instance** and install Jenkins.
<img width="1920" height="1008" alt="image" src="https://github.com/user-attachments/assets/36c88bec-80d5-453d-a02a-8e1704417225" />

Install Java:

```bash
sudo apt update
sudo apt install fontconfig openjdk-21-jre
java -version
```
![Screenshot 2026-03-04 153944.png](attachment:7d10681e-372e-4302-b5e4-cf956fb50099:Screenshot_2026-03-04_153944.png)

Install Jenkins:

![Screenshot 2026-03-04 154203.png](attachment:3f493fe2-f446-450b-9ea2-a8e37fda8cde:Screenshot_2026-03-04_154203.png)

```bash
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc \
https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key

echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc]" \
https://pkg.jenkins.io/debian-stable binary/ | sudo tee \
/etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install jenkins
```
![Screenshot 2026-03-04 154818.png](attachment:10bb948a-efcf-4687-9a0e-0d715d795b82:Screenshot_2026-03-04_154818.png)
Allow **port 8080** in the security group to access Jenkins.

---

# Step 2 – Configure Jenkins
![image.png](attachment:7a544a78-d68d-4609-8955-c59039024d15:image.png)
![image.png](attachment:ca764c31-61d1-4a4b-adbc-b4d6a7544173:image.png)
1. Unlock Jenkins using the initial admin password
2. Install recommended plugins
3. Create a Jenkins user
4. Create a **Freestyle Project**

![image.png](attachment:865f8789-e941-467a-935d-ff581eac8969:image.png)
Configure the **GitHub repository URL** inside Jenkins.
![image.png](attachment:d40c1322-ae59-4d1a-9e49-83e4630dfe91:image.png)
Authentication is done using a **GitHub Personal Access Token**.
![Screenshot 2026-03-04 155855.png](attachment:98ae038a-5c65-4f73-bd08-f12de229d86c:Screenshot_2026-03-04_155855.png)
![Screenshot 2026-03-04 155937.png](attachment:d86085a1-d830-4315-971e-3d739b12b34a:Screenshot_2026-03-04_155937.png)
![image.png](attachment:15114143-0fa6-4cc3-94f2-475d42e0c4ad:image.png)
---

# Step 3 – Install Docker on Jenkins Server

```bash
sudo apt update
sudo apt install docker.io -y
```

Add Jenkins user to Docker group:
![image.png](attachment:5ab96d09-b262-4c0c-88b0-09809ec6670b:image.png)
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

Allow Jenkins to execute sudo commands without password:

```bash
sudo visudo
```

Add:

```bash
jenkins ALL=(ALL) NOPASSWD: ALL
```
![Screenshot 2026-03-04 171121.png](attachment:6fa851bb-c7aa-4229-80b6-5b1436e20f55:Screenshot_2026-03-04_171121.png)

---

# Step 4 – Jenkins Build Pipeline

The Jenkins job executes the following steps:

```bash
# remove existing repo if present
if [ -d "project_6_DevOps" ]; then
    rm -rf project_6_DevOps
fi

# clone repository
git clone https://github.com/ashish200214/project_6_DevOps.git

cd project_6_DevOps

# build application
mvn clean install -DskipTests

# build docker image
docker build -t $DOCKER_USERNAME/project5 .

# scan docker image
trivy image --severity CRITICAL --exit-code 1 $DOCKER_USERNAME/project5

# login to docker hub
echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin

# push image
docker push $DOCKER_USERNAME/project5

# deploy to kubernetes
scp -o StrictHostKeyChecking=no -i $SSH_KEY deployment.yml $SSH_USER@52.91.20.184:.

ssh -o StrictHostKeyChecking=no -i $SSH_KEY $SSH_USER@52.91.20.184 << EOF
kubectl apply -f deployment.yml
EOF
```

---

# Step 5 – Kubernetes Cluster Setup

Create **two EC2 instances**:

* Master Node
* Worker Node

Install Kubernetes components on both nodes:

* kubeadm
* kubelet
* kubectl
* containerd

Disable swap and configure kernel modules.

---

# Step 6 – Initialize Master Node

Run on master node:

```bash
sudo kubeadm init
```

Configure kubectl:

```bash
mkdir -p $HOME/.kube
sudo cp /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

Install network plugin (Calico):

```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.26.0/manifests/calico.yaml
```

Generate worker join command:

```bash
kubeadm token create --print-join-command
```

Run the join command on worker node.

---

# Step 7 – Deploy Application to Kubernetes

Create a deployment file.

Example deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: project5
spec:
  replicas: 2
  selector:
    matchLabels:
      app: project5
  template:
    metadata:
      labels:
        app: project5
    spec:
      containers:
      - name: project5
        image: ashish200214/project5
        ports:
        - containerPort: 8080
```

---

# Step 8 – Expose Application using NodePort

Example service:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: project5-service
spec:
  type: NodePort
  selector:
    app: project5
  ports:
  - port: 80
    targetPort: 8080
    nodePort: 30007
```

Access application using:

```
http://<worker-node-ip>:30007
```

---

# Security Scan using Trivy

Trivy scans Docker images for vulnerabilities.

Example command:

```bash
trivy image --severity CRITICAL --exit-code 1 image_name
```

If critical vulnerabilities are found, the Jenkins build will fail.

---

# Result

The CI/CD pipeline successfully:

* Builds the Java application
* Creates a Docker image
* Scans for vulnerabilities
* Pushes the image to Docker Hub
* Deploys the application to Kubernetes automatically

---

# Technologies Used

* Jenkins
* Docker
* Trivy
* Kubernetes
* Maven
* GitHub
* AWS EC2

---

# Key Benefits

* Automated CI/CD pipeline
* Containerized application deployment
* Security vulnerability scanning
* Scalable Kubernetes deployment
* Cloud based infrastructure

---

This project demonstrates a **real-world DevOps workflow for automated application delivery using containerization and Kubernetes orchestration.**
