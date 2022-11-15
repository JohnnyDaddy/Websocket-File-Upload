# Introduction 
웹 소켓을 통한 파일 업로드 및 프로그레스 표시 예제


# Getting Started
1. backend를 8080으로 구동
- spring dashboard extension 사용
2. frontend를 8081로 구동
```
cd frontend
npm i
npm run serve
```

# Build and Test
http://localhost:8081 로 접속하여 파일 선택 후, 프로그레스가 업데이트 되는 모습을 확인하고 실제 uploadPath directory에 전송한 파일이 저장되었는지 확인.

# Contribute
본 예제는 tomcat websocket을 활용한 심플한 예제이므로
확장성을 위해 spring websocket이나 STOMP 등으로 전환하는 것을 고려해야 함.
- (참고)[https://brunch.co.kr/@springboot/695]
- 소스 내의 TODO 확인