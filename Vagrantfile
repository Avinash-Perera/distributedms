Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/focal64"
  config.vm.box_check_update = false

  # Master Node
  config.vm.define "k3s-master" do |master|
    master.vm.hostname = "k3s-master"
    master.vm.network "private_network", ip: "192.168.50.10"
    master.vm.provider "virtualbox" do |vb|
      vb.memory = "4096"
      vb.cpus = 2
    end
  end

  # Worker Node 1
  config.vm.define "k3s-worker-1" do |worker1|
    worker1.vm.hostname = "k3s-worker-1"
    worker1.vm.network "private_network", ip: "192.168.50.11"
    worker1.vm.provider "virtualbox" do |vb|
      vb.memory = "2048"
      vb.cpus = 1
    end
  end

  # Worker Node 2
  config.vm.define "k3s-worker-2" do |worker2|
    worker2.vm.hostname = "k3s-worker-2"
    worker2.vm.network "private_network", ip: "192.168.50.12"
    worker2.vm.provider "virtualbox" do |vb|
      vb.memory = "2048"
      vb.cpus = 1
    end
  end
end
