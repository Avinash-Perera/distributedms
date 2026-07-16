terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
  }
}

provider "kubernetes" {
  config_path    = var.kube_config_path
  config_context = var.kube_context
}

# In a true Terraform setup, each resource would be mapped to a kubernetes_deployment 
# or kubernetes_service block. 
# For this POC, we use a null_resource to apply the multi-document YAML manifests
# we created in the k8s/ directory, proving the deployment pipeline.

resource "null_resource" "apply_k8s_manifests" {
  # Trigger re-run if any yaml file changes
  triggers = {
    configmaps    = filemd5("${path.module}/../k8s/01-configmaps.yaml")
    infrastructure = filemd5("${path.module}/../k8s/02-infrastructure.yaml")
    microservices = filemd5("${path.module}/../k8s/03-microservices.yaml")
  }

  provisioner "local-exec" {
    command = "kubectl apply -f ${path.module}/../k8s/"
  }
}

resource "null_resource" "delete_k8s_manifests" {
  provisioner "local-exec" {
    when    = destroy
    command = "kubectl delete -f ${path.module}/../k8s/"
  }
}
