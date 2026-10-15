#!/usr/bin/env node
/*
 * Kiem tra may co du phien ban Java / Node.js / SQL Server de chay FootStyle hay chua, va noi RO cach sua neu thieu.
 * Chi dung Node co san (khong can npm install) nen chay duoc truoc ca buoc cai thu vien. Duoc goi tu chay-du-an.bat / chay-du-an.sh:
 *     node scripts/kiem-tra-moi-truong.cjs              kiem tra Node, Java, SQL Server
 *     node scripts/kiem-tra-moi-truong.cjs --chi-node   chi kiem tra Node
 * Ma thoat: 0 = chay duoc (co the kem canh bao), 1 = thieu / qua cu, phai sua truoc khi chay.
 * Muon bo qua hoan toan (vi du may dac biet, kiem tra nham): dat bien moi truong FOOTSTYLE_BO_QUA_KIEM_TRA=1.
 *
 * Thong bao viet khong dau vi cua so cmd cua Windows hay loi font tieng Viet.
 *
 * Phien ban toi thieu (xem HUONG_DAN_CHAY.md, muc "Phien ban duoc ho tro"):
 *   - Java (JDK, khong phai JRE) 17 tro len. Da thu JDK 17 va 21; JDK 22 den 27 dung duoc nho pom.xml da dat Lombok / Byte Buddy moi.
 *   - Node.js 20.19+ hoac 22.12+ (Vite 8 yeu cau). Chay lenh `npm test` can Node 22.12 tro len.
 *   - SQL Server 2016 tro len, bat TCP/IP cong 1433 (hoac sua duong dan trong application.properties / bien DB_URL).
 */
'use strict'

const fs = require('node:fs')
const net = require('node:net')
const path = require('node:path')
const { spawnSync } = require('node:child_process')

const JAVA_TOI_THIEU = 17
const JAVA_DA_THU_TOI_DA = 27

/* ===================== Node.js ===================== */

/** "22.12.0" -> true. Vite 8: ^20.19 || >=22.12 (Node 21 va 22.0-22.11 khong dung duoc). */
function nodeDuDung(phienBan) {
  const [a, b] = String(phienBan).replace(/^v/, '').split('.').map(Number)
  if (!Number.isFinite(a) || !Number.isFinite(b)) return false
  return (a === 20 && b >= 19) || (a === 22 && b >= 12) || a >= 23
}

/** Node chay duoc giao dien nhung bo kiem thu (vitest 5 + jsdom 30) can Node 22.12 tro len. */
function nodeChayDuocKiemThu(phienBan) {
  const [a, b] = String(phienBan).replace(/^v/, '').split('.').map(Number)
  return (a === 22 && b >= 12) || a >= 23
}

function kiemTraNode(phienBan = process.versions.node) {
  const loi = []
  const canhBao = []
  if (!nodeDuDung(phienBan)) {
    loi.push(
      `Node.js ${phienBan} qua cu hoac khong duoc ho tro (can 20.19 tro len hoac 22.12 tro len). ` +
        'Cai Node.js ban LTS moi nhat tai https://nodejs.org roi mo lai cua so dong lenh.'
    )
  } else if (!nodeChayDuocKiemThu(phienBan)) {
    canhBao.push(`Node.js ${phienBan} chay duoc giao dien, nhung lenh "npm test" can Node 22.12 tro len (nen dung ban LTS moi nhat).`)
  }
  return { thongTin: `Node.js ${phienBan}`, loi, canhBao }
}

/* ===================== Java ===================== */

/** Lay so phien ban chinh tu "java.specification.version": "1.8" -> 8, "17" -> 17, "21" -> 21. */
function soPhienBanJava(spec) {
  const m = String(spec ?? '').trim().match(/^(\d+)(?:\.(\d+))?/)
  if (!m) return null
  return m[1] === '1' && m[2] ? Number(m[2]) : Number(m[1])
}

/** Doc ket qua cua `java -XshowSettings:properties -version` (in ra stderr). */
function phanTichJava(dauRa) {
  const lay = (khoa) => {
    const m = String(dauRa ?? '').match(new RegExp(`^\\s*${khoa.replace(/\./g, '\\.')}\\s*=\\s*(.+?)\\s*$`, 'm'))
    return m ? m[1] : null
  }
  return {
    chinh: soPhienBanJava(lay('java.specification.version')),
    day_du: lay('java.version'),
    home: lay('java.home'),
    nhaCungCap: lay('java.vendor'),
  }
}

function chayJava(lenh) {
  const r = spawnSync(lenh, ['-XshowSettings:properties', '-version'], { encoding: 'utf8', timeout: 20000, windowsHide: true })
  if (r.error || r.status !== 0) return null
  return phanTichJava(`${r.stderr ?? ''}\n${r.stdout ?? ''}`)
}

/** Co trinh bien dich javac canh java khong? (may chi cai JRE thi `mvnw` bao "No compiler is provided"). */
function coJavac(home, fsDung = fs) {
  if (!home) return true // khong ro thi khong chan
  return ['javac', 'javac.exe'].some((t) => fsDung.existsSync(path.join(home, 'bin', t)))
}

/**
 * mvnw dung JAVA_HOME truoc, khong co thi dung `java` trong PATH. Lam y het de bao dung cai ma Maven se dung.
 * @param moiTruong process.env; @param chay ham chay java (de test)
 */
function kiemTraJava(moiTruong = process.env, chay = chayJava, fsDung = fs, heDieuHanh = process.platform) {
  const loi = []
  const canhBao = []
  const tenJava = heDieuHanh === 'win32' ? 'java.exe' : 'java'
  const javaHome = (moiTruong.JAVA_HOME ?? '').trim().replace(/^"|"$/g, '')
  let lenh = 'java'
  let nguon = 'trong PATH'
  if (javaHome) {
    lenh = path.join(javaHome, 'bin', tenJava)
    nguon = `JAVA_HOME=${javaHome}`
    if (!fsDung.existsSync(lenh)) {
      loi.push(
        `Bien JAVA_HOME dang tro toi "${javaHome}" nhung o do khong co bin/${tenJava}. ` +
          'Sua JAVA_HOME cho dung thu muc JDK (vi du C:\\Program Files\\Java\\jdk-21), hoac xoa bien JAVA_HOME de dung java trong PATH.'
      )
      return { thongTin: 'Java: khong tim thay', loi, canhBao }
    }
  }
  const kq = chay(lenh)
  if (!kq || kq.chinh === null) {
    loi.push(
      `Khong chay duoc lenh java (${nguon}). Cai JDK ${JAVA_TOI_THIEU} tro len (vi du Temurin / Oracle JDK 21 hoac 25) roi mo lai cua so dong lenh; ` +
        'neu da cai thi dat bien JAVA_HOME tro toi thu muc JDK.'
    )
    return { thongTin: 'Java: khong tim thay', loi, canhBao }
  }
  const thongTin = `Java ${kq.day_du ?? kq.chinh}${kq.nhaCungCap ? ` (${kq.nhaCungCap})` : ''}, ${nguon}`
  if (kq.chinh < JAVA_TOI_THIEU) {
    loi.push(
      `Java ${kq.chinh} qua cu, du an can JDK ${JAVA_TOI_THIEU} tro len (${nguon}). ` +
        'Cai JDK 21 hoac 25 roi tro JAVA_HOME toi do (neu may con ban cu trong PATH / JAVA_HOME thi sua lai).'
    )
  } else if (!coJavac(kq.home, fsDung)) {
    loi.push(
      `Java ${kq.chinh} o day chi la JRE (khong co javac nen khong bien dich duoc backend). Cai ban JDK day du (khong phai JRE) roi tro JAVA_HOME toi do.`
    )
  } else if (kq.chinh > JAVA_DA_THU_TOI_DA) {
    canhBao.push(`Java ${kq.chinh} moi hon cac ban da thu (17 den ${JAVA_DA_THU_TOI_DA}). Neu backend bao loi, dung JDK 21 hoac 25 (ban LTS).`)
  }
  return { thongTin, loi, canhBao }
}

/* ===================== SQL Server ===================== */

/**
 * Dia chi SQL Server ma backend se dung: bien moi truong DB_URL, neu khong co thi gia tri mac dinh trong application.properties.
 * Tra ve { host, cong } hoac null neu dung instance co ten (cong dong, khong do duoc) / khong doc duoc.
 */
function diaChiSqlServer(moiTruong, noiDungProperties) {
  let url = (moiTruong.DB_URL ?? '').trim()
  if (!url && noiDungProperties) {
    const m = noiDungProperties.match(/^\s*spring\.datasource\.url\s*=\s*(?:\$\{DB_URL:)?(jdbc:sqlserver:\/\/[^\s}]+)/m)
    if (m) url = m[1]
  }
  const m = url.match(/^jdbc:sqlserver:\/\/([^;:\\/]+)(?:\\([^;:]+))?(?::(\d+))?/i)
  if (!m) return null
  if (m[2] || /instanceName=/i.test(url)) return null
  return { host: m[1], cong: m[3] ? Number(m[3]) : 1433 }
}

function thuNoiTcp(host, cong, msChoPhep = 2500) {
  return new Promise((xong) => {
    const o = net.connect({ host, port: cong })
    let daXong = false
    const ketThuc = (ok, lyDo) => {
      if (daXong) return
      daXong = true
      o.destroy()
      xong({ ok, lyDo })
    }
    o.setTimeout(msChoPhep, () => ketThuc(false, 'het thoi gian cho'))
    o.once('connect', () => ketThuc(true))
    o.once('error', (e) => ketThuc(false, e.code ?? e.message))
  })
}

async function kiemTraSqlServer(moiTruong, noiDungProperties, thu = thuNoiTcp) {
  const dia = diaChiSqlServer(moiTruong, noiDungProperties)
  if (!dia) return { thongTin: 'SQL Server: bo qua (dung instance co ten hoac DB_URL khac dang thuong)', loi: [], canhBao: [] }
  const kq = await thu(dia.host, dia.cong)
  if (kq.ok) return { thongTin: `SQL Server ${dia.host}:${dia.cong} dang mo`, loi: [], canhBao: [] }
  return {
    thongTin: `SQL Server ${dia.host}:${dia.cong} khong ket noi duoc (${kq.lyDo})`,
    loi: [],
    canhBao: [
      'Backend se khong len duoc cho toi khi SQL Server san sang: bat dich vu SQL Server, bat TCP/IP cong 1433 trong "SQL Server Configuration Manager" ' +
        '(ban Express mac dinh tat TCP/IP), da chay database/SD_17.sql va database/SD_17_du_lieu_va_nang_cap.sql, dung tai khoan / mat khau trong ' +
        'backend/src/main/resources/application.properties (hoac bien DB_URL, DB_USERNAME, DB_PASSWORD).',
    ],
  }
}

/* ===================== Chay ===================== */

async function main(argv = process.argv.slice(2), moiTruong = process.env) {
  if (['1', 'true', 'yes'].includes(String(moiTruong.FOOTSTYLE_BO_QUA_KIEM_TRA ?? '').trim().toLowerCase())) {
    console.log('[FootStyle] Bo qua buoc kiem tra moi truong (FOOTSTYLE_BO_QUA_KIEM_TRA).')
    return 0
  }
  const chiNode = argv.includes('--chi-node')
  const goc = path.resolve(__dirname, '..')
  const ketQua = [kiemTraNode()]
  if (!chiNode) {
    ketQua.push(kiemTraJava())
    let props = ''
    try {
      props = fs.readFileSync(path.join(goc, 'backend', 'src', 'main', 'resources', 'application.properties'), 'utf8')
    } catch {
      /* khong doc duoc thi dung dia chi mac dinh */
    }
    ketQua.push(await kiemTraSqlServer(moiTruong, props))
  }
  let coLoi = false
  for (const k of ketQua) {
    const dau = k.loi.length ? '[LOI]  ' : k.canhBao.length ? '[CHU Y]' : '[OK]   '
    console.log(`[FootStyle] ${dau} ${k.thongTin}`)
    for (const l of k.loi) console.log(`[FootStyle]        -> ${l}`)
    for (const c of k.canhBao) console.log(`[FootStyle]        -> ${c}`)
    if (k.loi.length) coLoi = true
  }
  if (coLoi) console.log('[FootStyle] Sua cac loi [LOI] o tren roi chay lai.')
  return coLoi ? 1 : 0
}

module.exports = { main, nodeDuDung, nodeChayDuocKiemThu, kiemTraNode, soPhienBanJava, phanTichJava, coJavac, kiemTraJava, diaChiSqlServer, kiemTraSqlServer, thuNoiTcp }

if (require.main === module) {
  main().then((ma) => process.exit(ma), (e) => {
    console.log(`[FootStyle] Khong kiem tra duoc moi truong (${e.message}), cu thu chay tiep.`)
    process.exit(0)
  })
}
