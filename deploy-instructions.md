# Deploy & Fix 401 Instructions for MobaXterm

## 1. Build
```bash
mvn clean package -DskipTests
```

## 2. Deploy WAR to WildFly
```
scp target/demo-0.0.1-SNAPSHOT.war user@server:/path/to/wildfly/standalone/deployments/
```

## 3. On Server (MobaXterm)
```bash
cd /path/to/wildfly/standalone/data/
sudo mkdir -p demo-db
sudo chown -R $(whoami):$(whoami) demo-db/
sudo chmod 755 demo-db/
# Copy demodb.mv.db if exists from local
```

## 4. Test API
```bash
curl -v http://localhost:8080/api/health
# Expected: {\"status\":\"OK\",\"userCount\":2+}

curl -v -X POST http://localhost:8080/api/register \\
  -H \"Content-Type: application/json\" \\
  -d '{\"username\":\"test\",\"password\":\"test\",\"email\":\"test@test.com\"}'

curl -v -X POST http://localhost:8080/api/login \\
  -H \"Content-Type: application/json\" \\
  -b cookies.txt -c cookies.txt \\
  -d '{\"username\":\"test\",\"password\":\"test\"}'
```

## 5. Check Logs
```
tail -f /path/to/wildfly/standalone/log/server.log | grep -E \"(Initializing|Loading user|Login attempt|Failed)\"
```

## 6. Browser Test
http://server-ip:8080 → Register/Login

**401 Fix**: DB permissions + logs will show exact cause (user load fail).
