variable "kube_config_path" {
  description = "Path to your local kubeconfig file"
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "The Kubernetes context to use (e.g., docker-desktop)"
  type        = string
  default     = "docker-desktop"
}
