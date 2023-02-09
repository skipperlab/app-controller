### install NFS
sudo apt-get install nfs-kernel-server nfs-common
mkdir /opt/share
chown nobody:nogroup /opt/share
chmod 755 /opt/share
cat <<EOF > /etc/exports
/opt/share *(rw,sync,no_subtree_check)
EOF
exportfs -a
systemctl restart nfs-server

### install NFS-provider
helm repo add nfs-subdir-external-provisioner https://kubernetes-sigs.github.io/nfs-subdir-external-provisioner/
helm install nfs-subdir-external-provisioner nfs-subdir-external-provisioner/nfs-subdir-external-provisioner \
> --set nfs.server=141.164.42.200 \
> --set nfs.path=/opt/share
# kubectl patch storageclass nfs-client -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

### install NFS-PVC
kubectl apply -f <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-connectors
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: nfs-client
  resources:
    requests:
      storage: 5Gi
EOF