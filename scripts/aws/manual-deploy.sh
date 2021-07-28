#echo "Workaround on DNS resolution problem"
#sudo sed -i /etc/resolv.conf -e 's/nameserver.*/nameserver 8.8.8.8/'

echo "Install docker"
sudo yum -y install docker
sudo systemctl enable docker
sudo systemctl start docker

echo "Download pn-delivery"
export ECR_REGION=eu-central-1
export ECR_ACCOUNT_ID=558518206506
export ECR_URL=$ECR_ACCOUNT_ID.dkr.ecr.$ECR_REGION.amazonaws.com

env | grep ECR
aws ecr get-login-password --region $ECR_REGION | sudo docker login --username AWS --password-stdin $ECR_URL

sudo docker pull $ECR_URL/pn-delivery:latest

echo "Run pn-delivery"
sudo docker run -p 8080:8080 --rm --name pn-delivery -d $ECR_URL/pn-delivery:latest
