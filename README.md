### What's this?

A simple "homework assignment" which used to be assigned to applicants for java backend developer role in Zonky.cz.

### What does the original assignment look like?

In czech (sorry, no translation) it states:

> Zkuste naprogramovat nástroj, který bude každých 5m kontrolovat nové půjčky na Zonky.cz tržišti a ty vypíše.
> API Zonky tržiště je dostupné na adrese https://api.zonky.cz/loans/marketplace, dokumentace pak na http://docs.zonky.apiary.io/#.
> Výběr technologií necháme na Vás, jenom ať je to prosím v Jave.
>
> Speciálně přihlížíme k dobré testovatelnosti a čistotě kódu a naopak nemáme moc rádi over-engineered řešení.

### Why did I implement it?

I just wanted to try to implement the homework assignment as I wanted to see how easy or hard it might be to correctly check for new loans while respecting paging, filtering & sorting. Without stable sorting, filtering & paging, it's actually quite easy to accidentally return incomplete data.

### Why clojure and not Java?

Because. I've seen way too many java/spring solutions.

Clojure offers interactive development, simplicity, persistent data structures, plenty of useful functions to process your data. Unlike Java.

### Where are the tests?

Not here. Maybe later.

### Any lesson learned?

Normally I tend not to even think about transducers. Threading macros (such as `->`, `->>` etc.), `map`, `filter`, `take`, `drop` usually work just fine. But since lazy sequences internally perform chunking, it might results in evaluating ("pre-calculating") more items of the threading "stream" than actually needed. 

Unfortunately that effect of lazy seq chunking is not acceptable when it results in up to 31 unnecessary sequential expensive API calls. That's why I had to implement the transformation chain using more effective `transduce` approach instead.

### Building uberjar

```bash
clj -T:build ci
```

### Running uberjar

```bash
java -jar target/zonky-homework-1.0.0-SNAPSHOT.jar
```

