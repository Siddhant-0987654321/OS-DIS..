#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include<netdb.h>
#include<netinet/in.h>
//#define SERVER_ADDR "172.18.0.2"
char SERVER_ADDR[16]="";
#define LINE_MAX_SIZE 1024
int pausetime = 3;
int num_of_trans = 10;

char* getServerAddr()
{
	struct hostent* host;
	host = gethostbyname("server");
	const char *hostip = inet_ntoa(*((struct in_addr*)host->h_addr))    ;
	strcpy(SERVER_ADDR,hostip);
	return SERVER_ADDR;
}
void contactServer(int port,char* rcvmsg)
{
	//创建套接字
	int sock = socket(AF_INET, SOCK_STREAM, 0);
	//向服务器（特定的IP和端口）发起请求
	struct sockaddr_in serv_addr;
	memset(&serv_addr, 0, sizeof(serv_addr));  //每个字节都用0填充
	serv_addr.sin_family = AF_INET;  //使用IPv4地址
	serv_addr.sin_addr.s_addr = inet_addr(SERVER_ADDR);  //具体的IP地址
	serv_addr.sin_port = htons(port);  //端口
	connect(sock, (struct sockaddr*)&serv_addr, sizeof(serv_addr));



	send(sock,"LINE\n",5*(sizeof(char)),0);
	int len = recv(sock,rcvmsg,LINE_MAX_SIZE,0);
	rcvmsg[len]='\0';

	//printf("Message form server: %s", rcvmsg);

	//关闭套接字
	close(sock);
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

//return 0: not in file; return 1: in file.
int isInFile(char* str,char* filename)
{
	FILE *fp; 
	char line[LINE_MAX_SIZE+1];		   //每行最大读取的字符数
	if((fp = fopen(filename,"r")) == NULL) //判断文件是否存在及可读
	{ 
		//printf("can't open file!"); 
		return 0; 
	} 
	char rcvmsg[LINE_MAX_SIZE+1] = "";
	while (!feof(fp)) 
	{ 
		fgets(line,LINE_MAX_SIZE,fp);  //读取一行
		if(strcmp(str,strcat(str2upper(line),"\n"))==0)
		{
			fclose(fp);
			return 1;
		}
	}
	fclose(fp);				 //关闭文件
	return 0;
}
int main(int argn,char** argv){
	char* filename = argv[1];
	int port = atoi(argv[2]);
	getServerAddr();
	char rcvmsg[LINE_MAX_SIZE+1] = "";
	int i;
	for(i = 0;i<num_of_trans;i++)
	{
		contactServer(port,rcvmsg);
		if(isInFile(rcvmsg,filename))
			printf("OK\n");
		else
			printf("MISSING\n");
		sleep(pausetime);
	}
	return 0; 
}



	
