#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include<netdb.h>
#include<netinet/in.h>
//#define SERVER_ADDR "172.18.0.2"
char SERVER_ADDR[16] = "";
#define LINE_MAX_SIZE 1024

char* getServerAddr()
{
	struct hostent* host;
	host = gethostbyname("server");
	const char *hostip = inet_ntoa(*((struct in_addr*)host->h_addr));
	strcpy(SERVER_ADDR,hostip);
	return SERVER_ADDR;
}
char* str2upper(char* str)
{
	char* rtn = str;
	while(*str!='\0')
	{
		*str = toupper(*str);
		str++;
	}
	return rtn;
}
int main(int argn,char** argv){
	char* filename = argv[1];
	int port = atoi(argv[2]);

	getServerAddr();


	//创建套接字
	int serv_sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	//将套接字和IP、端口绑定
	struct sockaddr_in serv_addr;
	memset(&serv_addr, 0, sizeof(serv_addr));  //每个字节都用0填充
	serv_addr.sin_family = AF_INET;  //使用IPv4地址
	serv_addr.sin_addr.s_addr = inet_addr(SERVER_ADDR);  //具体的IP地址
	serv_addr.sin_port = htons(port);  //端口
	bind(serv_sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr));



	while(1)
	{
		FILE *fp; 
		char line[LINE_MAX_SIZE+1];		   //每行最大读取的字符数
		if((fp = fopen(filename,"r")) == NULL) //判断文件是否存在及可读
		{ 
			printf("can't open file!"); 
			return -1; 
		} 
		char rcvmsg[LINE_MAX_SIZE+1] = "";
		while (1) 
		{ 
			fgets(line,LINE_MAX_SIZE-1,fp);  //读取一行
			if(feof(fp))
				break;
			str2upper(strcat(line,"\n"));
			respondClient(serv_sock,rcvmsg,line);
		} 
		fclose(fp);				 //关闭文件
	}
	close(serv_sock);
	return 0; 
}
int respondClient(int serv_sock,char* rcvmsg,char* sndmsg){

	///listen，成功返回0，出错返回-1
	if(listen(serv_sock,20) == -1)
	{
		perror("listen");
		exit(1);
	}

	//接收客户端请求
	struct sockaddr_in clnt_addr;
	socklen_t clnt_addr_size = sizeof(clnt_addr);
	int clnt_sock = accept(serv_sock, (struct sockaddr*)&clnt_addr, &clnt_addr_size);
	int len = recv(clnt_sock, rcvmsg, LINE_MAX_SIZE,0);
	//printf("recv from client: %s\n",rcvmsg);
	if(strcmp("LINE\n",rcvmsg)==0)
	{
		//向客户端发送数据
		send(clnt_sock, sndmsg, strlen(sndmsg), 0);
	}
	//printf("send to client: %s\n",sndmsg);

	//关闭套接字
	close(clnt_sock);

	return 0;
}
