# Müşteri Talep & İş Yönetim Sistemi

Müşterilerden gelen destek ve geliştirme taleplerinin uçtan uca yönetilmesini, önceliklendirilmesini ve yazılım ekipleri tarafından çözülmesini sağlayan web tabanlı bir iş akışı ve talep yönetim platformudur.

---

## Sistem Aktörleri (Roller)

Sistemde 5 temel kullanıcı rolü bulunmaktadır:
1. **Müşteri:** Yeni destek veya geliştirme talebi oluşturur, kendi taleplerini takip eder ve gerektiğinde revize eder.
2. **Ürün Yöneticisi:** Gelen talepleri iş etkisi, aciliyet vb. kriterlere göre değerlendirerek ürün yönetim skorunu (Talep Skoru) oluşturur ve ilgili departmana atar.
3. **Yazılım Yöneticisi:** Departmanına atanan talepleri teknik açıdan değerlendirir, Teknik Talep Skoru oluşturur ve işi yazılımcılara atar.
4. **Yazılımcı:** Kendisine atanan iş için süre tahmini yapar, kod geliştirmesini ve testlerini tamamlayarak işi sonuçlandırır.
5. **Admin:** Kullanıcıları onaylar, yeni kullanıcı oluşturur ve varolan kullanıcıları düzenler.

---

## İş Akış Süreci

Sistemdeki temel talep döngüsü şu şekildedir:

1. **Talep Oluşturma:** Müşteri sistem üzerinden yeni bir talep bildirir.
2. **Ürün Yönetimi Değerlendirmesi:** 
   * Ürün Yöneticisi talebi inceler ve bir Talep Skoru oluşturur.
   * Talebi ilgili departmana (örn. Yazılım) yönlendirir veya eksik bilgi durumunda müşteriye geri gönderir.
3. **Teknik Değerlendirme:** 
   * Yazılım Yöneticisi talebe istinaden Teknik Talep Skoru oluşturur.
   * İşi ilgili yazılımcıya atar veya gerekirse yeniden değerlendirilmesi için Ürün Yöneticisine geri yönlendirir.
4. **Geliştirme ve Test:**
   * Yazılımcı talebi alır, süre tahmini yapar, kod geliştirmesini ve gerekli testleri gerçekleştirir.
   * Testler başarısız olursa süreç geliştirme aşamasına geri döner.
   * Testler başarılı olursa yazılımcı talebin durumunu 'TAMAMLANDI' olarak günceller.

## Eklenen Özellikler

Sistemde 23 Temmuz 2026 itibariyle bulunan özellikler şu şekildedir:

1. **Müşteri:**
    * Talep başlığı, detayları, etkilenen kişi sayısı, talep türü ve talebe ilişkin belge gibi detayları girerek talep oluşturma/düzenleme.
    * Geçmişte yüklenen talepleri ve taleplerin durumlarını görüntüleme.
    * Ürün yöneticisi ile talep üzerinden doğrudan mesajlaşma.
    * Ürün yöneticisi tarafından geri gönderilen talepleri düzenleme, görüntüleme ve ürün yöneticisine geri gönderme.

2. **Ürün Sorumlusu:**
    * Müşteriler tarafından gelen talepleri inceleme, departmanlara atama ve iş etkisi, aciliyet gibi parametreleri girerek aciliyet skoru oluşturma.
    * Aciliyet skoru girilen talepleri iş akışına dönüştürerek ilgili departmanın yazılım yöneticisine atama.
    * Talepleri müşterilere geri gönderme.
    * Müşteri ve ekip ile aynı talep üzerinden ancak ayrı kanallarla haberleşme.
    * Geri gönderilen ve geri gönderdiği talepleri görüntüleme.
    * Sisteme kayıtlı şirketlerin puanlarını düzenleme.    

3. **Yazılım Sorumlusu:**
    * Ürün sorumlusu tarafından departmana atanan talepleri yazılımcıya atama ya da ürün sorumlusuna geri gönderme.
    * Taleplere ait ürün sorumlusu tarafından girilen talep skorlarını görüntüleme ve teknik parametreleri girerek teknik aciliyet skoru oluşturma.
    * Talepleri ürün yöneticisine geri gönderme.
    * Geri gönderdiği ve geri gönderilen talepleri görüntüleme.
    * Başı olduğu departmandaki yazılımcıları ve bu yazılımcılara atanan işleri görüntüleme.
    * Ekip ile talep üzerinden haberleşme, aynı talep üzerinde ürün yöneticisi ve müşteri arasındaki yazışmaları görüntüleme.

4. **Yazılımcı:**
    * Yazılım yöneticisi tarafından kendisine atanan talepleri görüntüleme.
    * Talepleri tamamlama, not ekleme ya da yazılım sorumlusuna geri gönderme.
    * Ekip ile talep üzerinden haberleşme, aynı talep üzerinde ürün yöneticisi ve müşteri arasındaki yazışmaları görüntüleme.
    * Talepleri yazılım yöneticisine geri gönderme.

5. **Admin:**
    * Sisteme kaydolan kullanıcıları onaylama ve reddetme.
    * Kullanıcı oluşturma, varolan kullanıcılar hakkında çeşitli detayları değiştirme.
    * Yeni şirket ve departman ekleme.
    * Kullanıcıların destek taleplerini görüntüleme ve tamamlama.