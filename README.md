# Müşteri Talep & İş Yönetim Sistemi

Müşterilerden gelen destek ve geliştirme taleplerinin uçtan uca yönetilmesini, önceliklendirilmesini ve yazılım ekipleri tarafından çözülmesini sağlayan web tabanlı bir iş akışı ve talep yönetim platformudur.

---

## Sistem Aktörleri (Roller)

Sistemde 4 temel kullanıcı rolü bulunmaktadır:
1. **Müşteri (Customer):** Yeni destek veya geliştirme talebi oluşturur, kendi taleplerini takip eder ve gerektiğinde revize eder.
2. **Ürün Yöneticisi / Sorumlusu (Product Manager):** Gelen talepleri iş etkisi, aciliyet vb. kriterlere göre değerlendirerek ürün yönetim skorunu (Talep Skoru) oluşturur ve ilgili departmana atar.
3. **Yazılım Yöneticisi / Sorumlusu (Software Manager):** Departmanına atanan talepleri teknik açıdan değerlendirir, Teknik Talep Skoru oluşturur ve işi yazılımcılara atar.
4. **Yazılımcı (Developer):** Kendisine atanan iş için süre tahmini yapar, kod geliştirmesini ve testlerini tamamlayarak işi sonuçlandırır.

---

## İş Akış Süreci (Workflow)

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

Sistemde 2 Temmuz 2026 (initial commit) itibariyle bulunan özellikler şu şekildedir:

1. **Müşteri:**
    * Talep başlığı, detayları, etkilenen kişi sayısı, talep türü ve talebe ilişkin belge gibi detayları girerek talep oluşturma/düzenleme.
    * Geçmişte yüklenen talepleri ve taleplerin durumlarını görüntüleme.

2. **Ürün Sorumlusu:**
    * Müşteriler tarafından gelen talepleri inceleme, departmanlara atama ve iş etkisi, aciliyet gibi parametreleri girerek aciliyet skoru oluşturma.
    * Aciliyet skoru girilen talepleri iş akışına dönüştürerek ilgili departmanın yazılım yöneticisine atama.
    * Talepleri müşterilere geri gönderme ya da reddetme.

3. **Yazılım Sorumlusu:**
    * Ürün sorumlusu tarafından departmana atanan talepleri yazılımcıya atama ya da ürün sorumlusuna geri gönderme.
    * Taleplere ait ürün sorumlusu tarafından girilen talep skorlarını görüntüleme ve teknik parametreleri girerek teknik aciliyet skoru oluşturma.

4. **Yazılımcı:**
    * Yazılım yöneticisi tarafından kendisine atanan talepleri görüntüleme.
    * Talepleri tamamlama, not ekleme ya da yazılım sorumlusuna geri gönderme.