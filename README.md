#Zadanie 1 — reflexia
Ciele

    Naučiť sa používať reflexiu v jazyku Java.
    Naučiť sa vytvárať modulárnu architektúru knižnice.

Úloha

Úlohou je vytvorenie knižnice realizujúcej perzistenciu – ukladanie objektov do databázy. Ide o veľmi jednoduchý ORM (object-relational mapper). Knižnica umožní definovať jednoduché dátové triedy reprezentujúce databázové tabuľky s tým, že objekt triedy bude zodpovedať riadku tabuľky.
API knižnice

API knižnice predstavuje trieda ReflectivePersistenceManager implementujúca rozhranie PersistenceManager. Jej konštruktor očakáva objekt pripojenia k databáze a spravované triedy. Trieda má poskytovať metódy:

    void createTables() — vytvorenie tabuliek v databáze (ak neexistujú),
    <T> Optional<T> get(Class<T> type, long id) — získanie entity z databázy na základe primárneho kľúča,
    <T> List<T> getAll(Class<T> type) — získanie všetkých entít daného typu,
    <T> List<T> getBy(Class<T> type, String fieldName, Object value) — získanie entít na základe hodnoty ľubovoľného stĺpca,
    long save(Object entity) — vloženie entity do databázy alebo jej aktualizácia,
    void delete(Object entity) — odstránenie entity z databázy (na základe primárneho kľúča).

Všetky metódy v prípade chyby majú vyhadzovať výnimku PersistenceException.

Metóda createTables() môže používať príkaz CREATE TABLE IF NOT EXISTS, ktorý zabezpečí, že tabuľky sa nevytvoria v prípade, že už existujú.

Metóda save() v sebe spája funkcionalitu SQL príkazov INSERT a UPDATE. Logika výberu príkazu je veľmi jednoduchá:

    Ak hodnota primárneho kľúča v objekte je rovná 0, potom predpokladáme, že objekt nebol načítaný z databázy, a teda je potrebné ho do databázy pridať (INSERT).
    Ak hodnota primárneho kľúča je rôzna od 0, potom predpokladáme, že objekt v databáze už je uložený a je potrebné ho aktualizovať (UPDATE).

V prípade vloženia nového objektu do databázy je nutné zároveň aktualizovať hodnotu jeho primárneho kľúča na tú, ktorá mu bola pridelená databázou. Tým sa zabezpečí, že opakované volanie save() s tým istým objektom už zabezpečí iba aktualizáciu.
Entitné triedy

Na označenie spravovaných tried a ich prvkov použijeme štandardné anotácie z Java Persistence API:

    @Entity pre triedy,
    @Id pre primárny kľúč,
    @Transient pre ignorované dátové členy,
    @ManyToOne pre referencie na iné triedy.

Všetky dátové členy, ktoré nie sú označené @Transient, budú uložené do databázy. PersistenceManager musí vedieť ukladať členské premenné základných primitívnych typov (int, double) a reťazcov (String).

Typ primárneho kľúča musí byť long. Hodnota primárneho kľúča sa nastaví automaticky pri prvom uložení objektu do databázy.

SQLite na rozdiel od iných databáz používa dynamické typovanie a typ stĺpca definuje iba odporúčaný typ dát s predvolený spôsob ukladania. Preto stačiť používať tieto typy stĺpcov (afinity):

    INTEGER pre všetky celočíselné typy,
    REAL pre float a double,
    TEXT pre String.

Ak trieda obsahuje referenciu označenú anotáciou @ManyToOne, v databáze sa tento vzťah uloží pomocou cudzieho kľúča. Pri vyberaní objektov z databázy sa automaticky vyberú aj referované objekty. Pritom polymorfizmus nemusí byť podporovaný.

Názov stĺpca sa v tejto iterácii musí zhodovať s názvom členskej premennej (aj v prípade cudzieho kľúča).
Návrh a implementácia

Pri implementácii sa snažte dosiahnuť modulárnosť vášho riešenia. Berte ohľad na to, že v ďalších zadaniach bude potrebné doplniť ďalší spôsob analýzy tried (anotačný procesor namiesto reflexie), a neskôr aj spôsob práce s entitami (generovanie kódu namiesto reflexie). Dobré členenie na moduly by vám malo pomôcť realizovať tieto zmeny.


Zadanie 2 — anotácie a proxy
Ciele

    Precvičiť získavanie metadát o kóde z anotácií.
    Naučiť sa spracovávať anotácie počas prekladu programu.
    Naučiť sa dynamicky vytvárať triedy v Jave pomocou dynamického proxy.

Úloha 1: Metadáta

Pomocou parametrov anotácií @Table a @Column bude možné špecifikovať detaily ukladania:

    názov tabuľky,
    názov stĺpca,
    podporu prázdnej hodnoty (NULL a NOT NULL),
    unikátnosť hodnoty (UNIQUE).

Tieto informácie sa použijú pri generovaní SQL príkazov. Nie je nutné implementovať vlastnú validáciu splnenia podmienok (napr. not null).

Nezabudnite aktualizovať implementáciu tak, aby sa pri generovaní SQL používali názvy špecifikované pomocou anotácií.
Úloha 2: Anotačný procesor

SQL príkaz pre vytvorenie tabuliek sa má generovať počas kompilácie. Konštruktor po novom nebude očakávať manažované triedy ako parametre a metóda createTables() bude využívať SQL kód vygenerovaný už počas prekladu.

Vygenerovaný SQL príkaz musí byť umiestnený tak, aby po zabalení výsledného programu do JAR a prenesení do iného prostredia, bolo možné ho správne načítať.

Anotačný procesor implementujte v projekte processor, ktorý už máte vytvorený v šablóne zadania.
Poznámka

Podstatné je uvedomiť si, že SQL súbor musí byť uložený tak, aby bolo možné ho použiť aj po tom, ako výslednú aplikáciu v podobe JAR súboru prenesiete do iného prostredia a spustíte. Najlepšie je, aby bol priamo súčasťou JAR súboru. Dá sa to dosiahnuť tak, že ho vytvorite pomocou Filer.createResource(StandardLocation.CLASS_OUTPUT, ...).

Na jeho načítanie v bežiacom programe môžete použiť metódu ClassLoader.getResourceAsStream(), ktorá hľadá súbor v CLASSPATH.
Úloha 3: Oneskorené načítanie

Aby sa predišlo načítavaniu referovaných objektov, ktoré nebudú používané, knižnica zabezpečí oneskorené načítanie (lazy fetching) – referovaný objekt bude načítaný až pri jeho prvom použití.

Oneskorené načítanie sa bude zapínať pomocou atribútu fetch anotácie @ManyToOne. Oneskorené načítanie stačí podporovať len pre prípady, kedy sa na objekt referuje cez rozhranie. To, ktorá konkrétna trieda má byť vytvorená pri načítaní z databázy, sa uvedie pomocou parametra anotácie, napríklad:

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Department.class)
    private IDepartment department;

Pri načítaní objektu, obsahujúceho takúto referenciu, sa referovaný objekt nebude načítavať z databázy, kým sa nepoužije. Napríklad:

Person p = manager.get(Person.class, 1);  // only Person is loaded
IDepartment d = p.getDepartment();        // d is just a proxy
d.getName();                              // now the real object is loaded

Nezabudnite ošetriť ukladanie objektov obsahujúcich proxy namiesto referencie.


Zadanie 3 — generovanie kódu a AOP
Ciele

    Precvičiť generovanie kódu počas kompilácie aplikácie.
    Naučiť sa používať aspektovo-orientované programovanie a AspectJ.

Úloha 1: Generovanie kódu

Aby sme optimalizovali prácu našej knižnice, presunieme čo najviac operácii do fázy prekladu. Okrem SQL príkazov na vytvárania tabuliek, môžeme generovať aj zdrojový kód pre načítavanie a ukladanie jednotlivých typov entít.

Pomocou anotačného procesora vygenerujte pre každý entítny typ novú triedu, ktorá bude zabezpečovať perzistenciu práve tohto typu — data access object (DAO). Vygenerujte tiež novú implementáciu rozhrania PersistenceManager, ktorá bude len na základe typu argumentu volať metódy príslušného DAO. Môžeme ju nazvať GeneratedPersistenceManager.

Využívajte možnosti oddelenia všeobecného kódu od špecifického. Napríklad umiestnéte všeobecný kód do abstraktných tried, od ktorých budú generované triedy dediť.
Úloha 2: Podpora transakcií

V kóde, ktorý bude používať našu knižnicu môže byť potrebné použiť transakcie. Napríklad ak niekoľko operácií nad databázou spolu súvisa a musia sa teda vykonať naraz, inak by sa databáza na chvíľu dostala do nekonzistentného stavu.

Definujte anotáciu @AtomicPersistenceOperation pomocou ktorej bude možné označiť používateľské metódy, ktoré budú zodpovedať jednej transakcii.

Pomocou AspectJ implementujte riešenie, ktoré automaticky vytvori transakciu pre takto označené metódy. Na konci ich vykonania metódy sa vykoná commit alebo rollback podľa toho, či metóda vyhodila výnimku. Nekonzistenciu hodnôt objektov po rollbacku riešiť netreba.
Poznámka

Pre jednoduchosť môžeme predpokladať, že v aplikácii bude existovať iba jedna inštancia triedy PersistenceManager. Pomocou aspektu môžete zachytiť vytváranie tejto inštancie.
