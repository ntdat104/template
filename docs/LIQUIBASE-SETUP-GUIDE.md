# Hướng dẫn setup Liquibase cho project Spring Boot mới

> Tài liệu này được rút ra từ cách project `template` (Template 9.3.0 / Spring Boot 4.1.1 / Java 21 / PostgreSQL) cấu hình Liquibase, viết lại cho một project Spring Boot **trắng, chưa có gì**.

**Mục lục**

1. [Dependencies](#1-dependencies)
2. [Cấu trúc thư mục](#2-cấu-trúc-thư-mục)
3. [master.xml](#3-masterxml)
4. [Changelog đầu tiên](#4-changelog-đầu-tiên)
5. [Cấu hình application.yml](#5-cấu-hình-applicationyml)
6. [Chạy lần đầu](#6-chạy-lần-đầu)
7. [Quy tắc vàng khi viết changeSet](#7-quy-tắc-vàng-khi-viết-changeset)
8. [Contexts: dev / prod / faker / test](#8-contexts-dev--prod--faker--test)
9. [Sinh changelog tự động từ entity JPA (liquibase:diff)](#9-sinh-changelog-tự-động-từ-entity-jpa-liquibasediff)
10. [Các lệnh Maven hay dùng](#10-các-lệnh-maven-hay-dùng)
11. [Xử lý sự cố thường gặp](#11-xử-lý-sự-cố-thường-gặp)
12. [Điều Template có mà bạn không nên copy](#12-điều-template-có-mà-bạn-không-nên-copy)
13. [Checklist](#13-checklist)

---

## 1. Dependencies

Spring Boot 4 có starter riêng cho Liquibase. Version do BOM của `spring-boot-starter-parent` quản lý nên **không khai báo version**:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Ở Spring Boot 3.x thì thay starter bằng:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Thêm vào `<properties>` (cần cho phần `liquibase:diff` ở mục 9):

```xml
<maven.build.timestamp.format>yyyyMMddHHmmss</maven.build.timestamp.format>
```

---

## 2. Cấu trúc thư mục

Giữ đúng convention của Template, vì Maven plugin và mọi tooling đều trỏ vào đường dẫn này:

```
src/main/resources/config/liquibase/
├── master.xml                                  # "mục lục", chỉ include, không chứa changeSet
└── changelog/
    ├── 00000000000000_initial_schema.xml       # sequence + bảng hệ thống
    ├── 20260911120000_added_entity_Product.xml
    └── 20260912093000_added_entity_constraints_Product.xml
```

**Quy ước tên file:** `<yyyyMMddHHmmss>_<mô_tả>.xml`

Timestamp làm prefix đảm bảo hai việc: thứ tự chạy luôn tuyến tính, và hai developer tạo changelog cùng ngày không đụng tên nhau.

---

## 3. `master.xml`

File này **không bao giờ chứa changeSet**. Nhiệm vụ của nó là khai báo property theo `dbms` và include các changelog.

Các `<property>` là điểm hay nhất của pattern Template: cùng một changelog chạy được trên PostgreSQL / MySQL / H2 mà không phải sửa gì — chỉ cần thêm dòng property cho dbms tương ứng.

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <property name="now"          value="current_timestamp" dbms="postgresql"/>
    <property name="floatType"    value="float4"            dbms="postgresql"/>
    <property name="clobType"     value="clob"              dbms="postgresql"/>
    <property name="blobType"     value="blob"              dbms="postgresql"/>
    <property name="uuidType"     value="uuid"              dbms="postgresql"/>
    <property name="datetimeType" value="datetime"          dbms="postgresql"/>
    <property name="timeType"     value="time(6)"           dbms="postgresql"/>

    <include file="config/liquibase/changelog/00000000000000_initial_schema.xml"
             relativeToChangelogFile="false"/>

    <!-- Thêm changelog mới vào đây, theo đúng thứ tự thời gian -->

</databaseChangeLog>
```

Lưu ý `relativeToChangelogFile="false"` → đường dẫn được tính từ classpath root, không phải từ vị trí `master.xml`.

---

## 4. Changelog đầu tiên

`src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <!-- Sequence dùng chung cho @GeneratedValue(strategy = GenerationType.SEQUENCE) -->
    <changeSet id="00000000000000" author="datnt">
        <createSequence sequenceName="sequence_generator" startValue="1050" incrementBy="50"/>
    </changeSet>

    <changeSet id="20260911120000-1" author="datnt">
        <createTable tableName="product">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" primaryKeyName="productPK" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="price" type="DECIMAL(21,2)"/>
            <column name="created_date" type="${datetimeType}">
                <constraints nullable="false"/>
            </column>
        </createTable>
        <dropDefaultValue tableName="product" columnName="created_date" columnDataType="${datetimeType}"/>
    </changeSet>

</databaseChangeLog>
```

Entity JPA tương ứng:

```java
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "sequence_generator", allocationSize = 50)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", precision = 21, scale = 2)
    private BigDecimal price;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    // getters / setters
}
```

`allocationSize` của `@SequenceGenerator` phải khớp `incrementBy` của `createSequence` (ở đây là 50), nếu không Hibernate sẽ sinh ID trùng.

---

## 5. Cấu hình `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: mydb
    password: ''
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      poolName: Hikari
      auto-commit: false
  jpa:
    hibernate:
      ddl-auto: none            # BẮT BUỘC: để Liquibase làm chủ schema
    open-in-view: false
  liquibase:
    change-log: classpath:config/liquibase/master.xml
    enabled: true
```

`ddl-auto: none` là dòng quan trọng nhất. Nếu để `update`, Hibernate và Liquibase sẽ tranh nhau sửa schema và bạn sẽ mất nhiều giờ debug những lỗi rất khó hiểu.

Tách cấu hình theo profile:

```yaml
# src/main/resources/config/application-dev.yml
spring:
  liquibase:
    # Bỏ 'faker' nếu không muốn nạp dữ liệu mẫu
    contexts: dev, faker
```

```yaml
# src/main/resources/config/application-prod.yml
spring:
  liquibase:
    contexts: prod
```

Nếu đặt file config trong `src/main/resources/config/` (như Template) thay vì thẳng `src/main/resources/`, khai báo thêm:

```yaml
spring:
  config:
    import: optional:classpath:config/application-dev.yml
```

Hoặc đơn giản hơn cho project mới: cứ để `application.yml`, `application-dev.yml`, `application-prod.yml` ngay tại `src/main/resources/` — Spring Boot tự nhận.

---

## 6. Chạy lần đầu

```bash
# Dựng PostgreSQL
docker run -d --name pg -p 5432:5432 \
  -e POSTGRES_DB=mydb \
  -e POSTGRES_USER=mydb \
  -e POSTGRES_HOST_AUTH_METHOD=trust \
  postgres:17

# Chạy app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Liquibase chạy tự động lúc startup và tạo 2 bảng metadata:

| Bảng | Vai trò |
|---|---|
| `databasechangelog` | Lịch sử các changeSet đã chạy + checksum MD5 |
| `databasechangeloglock` | Lock chống 2 instance migrate song song |

Kiểm tra:

```bash
docker exec -it pg psql -U mydb -d mydb -c \
  "SELECT id, author, filename, exectype FROM databasechangelog ORDER BY orderexecuted;"
```

---

## 7. Quy tắc vàng khi viết changeSet

**1. `id` + `author` + `filename` là khóa định danh** của changeSet trong `databasechangelog`. Đổi bất kỳ cái nào → Liquibase coi đó là changeSet mới và chạy lại.

**2. Không bao giờ sửa changeSet đã chạy ở môi trường khác.** Liquibase lưu checksum; sửa file sẽ làm app crash khi start:

```
liquibase.exception.ValidationFailedException: Validation Failed:
  1 changesets check sum
    config/liquibase/changelog/...xml::20260911120000-1::datnt was: 9:abc... but is now: 9:def...
```

Muốn thay đổi schema → **luôn tạo changeSet mới**.

**3. Một changeSet = một thay đổi logic.** Nhiều DDL statement trong cùng changeSet sẽ không rollback được sạch trên các DB không hỗ trợ transactional DDL.

**4. Luôn viết `rollback` cho những thay đổi Liquibase không tự suy ra được** (`<sql>`, `<update>`, `<delete>`):

```xml
<changeSet id="20260911130000-1" author="datnt">
    <sql>UPDATE product SET price = 0 WHERE price IS NULL</sql>
    <rollback/>   <!-- khai báo rỗng = chấp nhận không rollback được, nhưng có chủ đích -->
</changeSet>
```

**5. Dùng `preConditions` cho changeSet có rủi ro**, để chạy lại an toàn:

```xml
<changeSet id="20260911140000-1" author="datnt">
    <preConditions onFail="MARK_RAN">
        <not><tableExists tableName="product"/></not>
    </preConditions>
    <createTable tableName="product">...</createTable>
</changeSet>
```

**6. Tách changelog constraint (FK) ra file riêng.** Template làm vậy để tránh vấn đề thứ tự: tạo hết bảng trước, thêm khóa ngoại sau.

```xml
<changeSet id="20260912093000-2" author="datnt">
    <addForeignKeyConstraint baseColumnNames="category_id"
                             baseTableName="product"
                             constraintName="fk_product__category_id"
                             referencedColumnNames="id"
                             referencedTableName="category"/>
</changeSet>
```

---

## 8. Contexts: dev / prod / faker / test

Context là cơ chế để **cùng một `master.xml` chạy khác nhau theo môi trường**. Gắn `context="..."` vào changeSet, rồi chọn context lúc runtime qua `spring.liquibase.contexts`.

```xml
<!-- Chỉ chạy ở dev, khi context 'faker' được bật -->
<changeSet id="20260911150000-1" author="datnt" context="faker">
    <loadData file="config/liquibase/fake-data/product.csv"
              separator=";"
              tableName="product"
              usePreparedStatements="true">
        <column name="id" type="numeric"/>
        <column name="name" type="string"/>
        <column name="price" type="numeric"/>
    </loadData>
</changeSet>

<!-- Bảng chỉ dùng cho integration test -->
<changeSet id="20260911150000-2" author="datnt" context="test">
    <createTable tableName="date_time_wrapper">
        <column name="id" type="BIGINT">
            <constraints primaryKey="true" primaryKeyName="date_time_wrapperPK"/>
        </column>
        <column name="instant" type="timestamp"/>
    </createTable>
</changeSet>
```

Cách chọn:

| Môi trường | `spring.liquibase.contexts` | Kết quả |
|---|---|---|
| dev | `dev, faker` | schema + dữ liệu mẫu |
| prod | `prod` | chỉ schema, không có dữ liệu mẫu |
| test | `test` | schema + bảng phụ cho test |
| Maven plugin | `!test` | loại changeSet `test` khỏi diff |

Dấu `!` nghĩa là phủ định (exclude). ChangeSet **không có** attribute `context` sẽ chạy trong **mọi** context.

**Tắt hẳn Liquibase** để start app nhanh khi schema đã đúng:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring.liquibase.enabled=false
```

---

## 9. Sinh changelog tự động từ entity JPA (`liquibase:diff`)

Đây là phần tiết kiệm nhiều thời gian nhất: Liquibase so sánh **entity JPA** với **DB thật** rồi sinh ra changelog cho phần khác biệt.

Thêm plugin vào `<build><plugins>`:

```xml
<plugin>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-maven-plugin</artifactId>
    <version>${liquibase.version}</version>
    <configuration>
        <changeLogFile>config/liquibase/master.xml</changeLogFile>
        <diffChangeLogFile>${project.basedir}/src/main/resources/config/liquibase/changelog/${maven.build.timestamp}_changelog.xml</diffChangeLogFile>
        <driver>org.postgresql.Driver</driver>
        <url>jdbc:postgresql://localhost:5432/mydb</url>
        <username>mydb</username>
        <password></password>
        <defaultSchemaName/>
        <referenceUrl>hibernate:spring:com.example.domain?dialect=org.hibernate.dialect.PostgreSQLDialect&amp;hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy&amp;hibernate.implicit_naming_strategy=org.springframework.boot.hibernate.SpringImplicitNamingStrategy</referenceUrl>
        <verbose>true</verbose>
        <contexts>!test</contexts>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.liquibase.ext</groupId>
            <artifactId>liquibase-hibernate6</artifactId>
            <version>${liquibase.version}</version>
        </dependency>
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.2.0</version>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
            <version>3.1.1</version>
        </dependency>
        <dependency>
            <groupId>org.jboss.logging</groupId>
            <artifactId>jboss-logging</artifactId>
            <version>3.6.1.Final</version>
        </dependency>
    </dependencies>
</plugin>
```

Cần đổi:

- `com.example.domain` → package chứa các class `@Entity` của bạn
- `url` / `username` / `password` → DB thật (nên đưa vào `<properties>` hoặc profile Maven để không hardcode credential; project Template dùng `${liquibase-plugin.url}`, `${liquibase-plugin.username}`, `${liquibase-plugin.password}` khai báo trong profile `dev`/`prod`)

Trong `referenceUrl`, `hibernate.physical_naming_strategy=CamelCaseToUnderscoresNamingStrategy` là thứ khiến `createdDate` → `created_date`. Phải khớp với naming strategy app đang dùng, nếu không diff sẽ báo sai hàng loạt column.

**Cách dùng:**

```bash
# 1. Sửa/thêm entity JPA
# 2. Đảm bảo DB đang ở trạng thái đã migrate đầy đủ
./mvnw liquibase:diff
```

File mới xuất hiện ở `src/main/resources/config/liquibase/changelog/<timestamp>_changelog.xml`.

**Bắt buộc làm sau khi diff:**

1. **Đọc lại file sinh ra** — nó thường thừa index/constraint không cần, thiếu `dropDefaultValue`, hoặc sinh tên constraint xấu.
2. Sửa lại cho gọn, đổi `author` thành tên bạn, đổi `id` theo quy ước.
3. **Tự tay thêm `<include>` vào `master.xml`** — plugin không làm việc này.

`liquibase:diff` là công cụ hỗ trợ, không phải nguồn sự thật. Changelog là code, phải review như code.

---

## 10. Các lệnh Maven hay dùng

```bash
# Xem changeSet nào chưa chạy
./mvnw liquibase:status -Dliquibase.verbose=true

# Chạy migration mà không cần start app
./mvnw liquibase:update

# Xuất SQL ra file để DBA review trước khi lên prod (KHÔNG chạy vào DB)
./mvnw liquibase:updateSQL

# Rollback N changeSet gần nhất
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback về một tag
./mvnw liquibase:rollback -Dliquibase.rollbackTag=v1.0.0

# Giải phóng lock bị treo
./mvnw liquibase:releaseLocks

# Sinh changelog từ DB có sẵn (khi đưa Liquibase vào project đã chạy)
./mvnw liquibase:generateChangeLog

# Đánh dấu changeSet là đã chạy mà không thực thi
./mvnw liquibase:changelogSync
```

Nên thêm `liquibase:status` vào CI pipeline để phát hiện migration chưa apply trước khi deploy.

---

## 11. Xử lý sự cố thường gặp

**`Validation Failed: N changesets check sum`**

Bạn đã sửa một changeSet đã chạy. Hai cách:

- **Đúng (bắt buộc ở prod):** revert file về nguyên trạng, viết changeSet mới cho thay đổi mong muốn.
- **Chỉ ở máy local:** thêm `<validCheckSum>ANY</validCheckSum>` vào changeSet, hoặc drop DB rồi migrate lại từ đầu.

**`Waiting for changelog lock...` treo mãi**

App crash giữa lúc migrate, lock không được nhả:

```bash
./mvnw liquibase:releaseLocks
# hoặc trực tiếp:
# DELETE FROM databasechangeloglock;
```

**`liquibase:diff` báo khác biệt ở mọi column**

Naming strategy trong `referenceUrl` không khớp với app. Kiểm tra lại `hibernate.physical_naming_strategy` và `hibernate.implicit_naming_strategy`.

**Table đã tồn tại khi chạy changeSet đầu tiên**

Bạn đưa Liquibase vào DB đã có schema. Dùng `./mvnw liquibase:generateChangeLog` để sinh baseline từ DB hiện tại, rồi `./mvnw liquibase:changelogSync` để đánh dấu baseline là đã chạy.

**Muốn tên bảng của Liquibase khác mặc định**

```yaml
spring:
  liquibase:
    database-change-log-table: DATABASECHANGELOG
    database-change-log-lock-table: DATABASECHANGELOGLOCK
    default-schema: public
```

---

## 12. Điều Template có mà bạn không nên copy

Project `template` có file `src/main/java/io/tcbs/config/LiquibaseConfiguration.java` tự tạo bean `SpringLiquibase` để hỗ trợ **async start**: cấu hình `application.liquibase.async-start` (mặc định `true`) khiến migration chạy trên `taskExecutor` riêng, app không đợi migrate xong mới lên.

Hai lý do không nên mang sang project mới:

1. Nó phụ thuộc `tech.template:template-framework` (`SpringLiquibaseUtil`, `JHipsterConstants`) — tức phải kéo cả Template framework vào chỉ để dùng một tiện ích.
2. Async start có rủi ro thật: app nhận request khi schema chưa migrate xong.

**Với project mới: bỏ hẳn file này**, dùng auto-configuration của Spring Boot. Nó đủ cho gần như mọi nhu cầu và an toàn hơn. Chỉ cân nhắc async start khi startup time thực sự là vấn đề đo được.

Tương tự, project Template có profile Maven `no-liquibase` (kèm biến `${profile.no-liquibase}` ghép vào `spring.profiles.active`). Với project mới, dùng trực tiếp `-Dspring.liquibase.enabled=false` là đủ, không cần dựng cả bộ máy profile đó.

---

## 13. Checklist

- [ ] Thêm `spring-boot-starter-liquibase` + driver DB
- [ ] Thêm `maven.build.timestamp.format` vào `<properties>`
- [ ] Tạo `src/main/resources/config/liquibase/master.xml` (chỉ property + include)
- [ ] Tạo `changelog/00000000000000_initial_schema.xml` với `sequence_generator`
- [ ] `spring.jpa.hibernate.ddl-auto: none`
- [ ] `spring.liquibase.change-log: classpath:config/liquibase/master.xml`
- [ ] Tách `contexts` theo profile: `dev, faker` / `prod` / `test`
- [ ] Chạy app, verify 2 bảng `databasechangelog` + `databasechangeloglock`
- [ ] (Tuỳ chọn) Thêm `liquibase-maven-plugin` + `liquibase-hibernate6` cho `liquibase:diff`
- [ ] Thêm `liquibase:status` vào CI
- [ ] **Không** copy `LiquibaseConfiguration.java` từ Template

---

## Tham khảo trong project này

| Nội dung | File |
|---|---|
| Starter dependency | `pom.xml:139-142` |
| `liquibase-core` + version từ BOM | `pom.xml:311-315` |
| `liquibase-maven-plugin` đầy đủ | `pom.xml:858-900` |
| Biến `liquibase-plugin.*` theo profile | `pom.xml:955-958` (dev), `pom.xml:1068-1071` (prod) |
| Profile `no-liquibase` | `pom.xml:1060-1063` |
| `master.xml` với property theo dbms | `src/main/resources/config/liquibase/master.xml` |
| ChangeSet có `context="test"` | `src/main/resources/config/liquibase/changelog/00000000000000_initial_schema.xml:21` |
| Contexts theo profile | `src/main/resources/config/application-dev.yml:52`, `application-prod.yml:49` |
| Bean SpringLiquibase custom (async start) | `src/main/java/io/tcbs/config/LiquibaseConfiguration.java` |
| Property `application.liquibase.async-start` | `src/main/java/io/tcbs/config/ApplicationProperties.java:24-35` |

**Tài liệu ngoài**

- Liquibase change types: https://docs.liquibase.com/change-types/home.html
- Spring Boot + Liquibase: https://docs.spring.io/spring-boot/how-to/data-initialization.html
- Template database docs: https://www.template.tech/development/
