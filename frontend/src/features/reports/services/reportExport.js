export const printableColumns = section => section.columns.filter(c => c.type !== 'savings-action');
export const periodLabel = report => `${report.filters.from || 'Beginning of recorded history'} to ${report.filters.to || 'Latest recorded date'}`;
export const formatNumber = value => value == null ? 'Unavailable' : new Intl.NumberFormat('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
export function cellText(row, column) {
  const value = row[column.key];
  if (column.type === 'money') return formatNumber(value);
  if (value == null || value === '') return '—';
  if (column.type === 'datetime') return new Date(value).toLocaleString('en-GB');
  if (column.type === 'status') return String(value).replaceAll('_', ' ');
  return String(value);
}
export const filterLabel = report => Object.entries(report.filters).filter(([key,value]) => value && !['from','to'].includes(key)).map(([key,value]) => `${key}: ${value}`).join(' | ') || 'All records in period';
const fileName = report => `${report.type}-${report.generatedAt.slice(0,10)}`;
const summaryText = item => `${item.label}: ${item.money ? formatNumber(item.value) : item.value}`;

export async function createReportPdf(report) {
  const [{ jsPDF }, { autoTable }] = await Promise.all([import('jspdf'), import('jspdf-autotable')]);
  const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
  doc.setProperties({ title: report.title, author: report.organization, subject: periodLabel(report) });
  let y = 36;
  const table = options => {
    autoTable(doc, { startY: y, margin: { top: 36, bottom: 17, left: 12, right: 12 }, styles: { fontSize: 8, cellPadding: 2.3, overflow: 'linebreak' }, headStyles: { fillColor: [15,118,110] }, alternateRowStyles: { fillColor: [245,248,250] }, rowPageBreak: 'avoid', ...options });
    y = doc.lastAutoTable.finalY + 7;
  };
  if (report.member) table({ head: [['Member Code','Member Name','Department','Status']], body: [[report.member.code,report.member.name,report.member.department||'—',report.member.status]] });
  if (report.summary.length) table({ head: [['Summary',`Value (${report.currency})`]], body: report.summary.map(item=>[item.label,item.money?formatNumber(item.value):String(item.value)]), columnStyles: { 1: { halign: 'right' } } });
  for (const section of report.sections) {
    if (y > 170) { doc.addPage(); y=36; }
    doc.setFontSize(11); doc.setTextColor(30,41,59); doc.text(section.title,12,y); y+=4;
    const columns=printableColumns(section);
    table({ head:[columns.map(c=>c.label)], body:section.rows.length?section.rows.map(row=>columns.map(c=>cellText(row,c))):[[{content:'No records match the selected filters.',colSpan:columns.length}]], columnStyles:Object.fromEntries(columns.map((c,i)=>[i,c.type==='money'?{halign:'right'}:{}])) });
    if(section.totals.length)table({ body:section.totals.map(item=>[item.label,item.money?formatNumber(item.value):String(item.value)]) });
  }
  if(report.notes.length)table({ head:[['Report notes']],body:report.notes.map(note=>[note]) });
  const pages=doc.getNumberOfPages();
  for(let page=1;page<=pages;page++) {
    doc.setPage(page); doc.setTextColor(15,118,110); doc.setFontSize(14); doc.text(report.organization,12,12);
    doc.setTextColor(30,41,59); doc.setFontSize(11); doc.text(report.title,12,18);
    doc.setFontSize(8); doc.text(`Period: ${periodLabel(report)} | Currency: ${report.currency}`,12,24);
    doc.text(doc.splitTextToSize(filterLabel(report),270).slice(0,2),12,29);
    doc.setDrawColor(210,220,225); doc.line(12,196,285,196);
    doc.setFontSize(8);doc.text(`Generated: ${new Date(report.generatedAt).toLocaleString('en-GB')}`,12,202);
    doc.text(`Page ${page} of ${pages}`,285,202,{align:'right'});
  }
  return doc;
}

export async function createReportWorkbook(report) {
  const module=await import('exceljs');
  const ExcelJS=module.default||module;
  const workbook=new ExcelJS.Workbook();
  workbook.creator=report.organization;workbook.created=new Date(report.generatedAt);workbook.title=report.title;
  const sections=[{title:'Summary',columns:[{key:'label',label:'Summary'},{key:'value',label:`Value (${report.currency})`}],rows:report.summary,totals:[]},...report.sections];
  sections.forEach((section,index)=>{
    const sheet=workbook.addWorksheet(`${index+1} ${section.title}`.replace(/[\\/*?:\[\]]/g,' ').slice(0,31),{pageSetup:{paperSize:9,orientation:'landscape',fitToPage:true,fitToWidth:1,fitToHeight:0},headerFooter:{oddFooter:'&L'+report.organization+'&RPage &P of &N'}});
    const columns=printableColumns(section), width=Math.max(2,columns.length);
    const addHeading=(value,bold=false)=>{const row=sheet.addRow([value]);sheet.mergeCells(row.number,1,row.number,width);row.font={name:'Calibri',size:bold?14:10,bold,color:{argb:bold?'FF0F766E':'FF334155'}};row.alignment={wrapText:true,vertical:'middle'};row.height=bold?24:30;};
    addHeading(report.organization,true);addHeading(report.title,true);addHeading(`Period: ${periodLabel(report)} | Currency: ${report.currency}`);addHeading(`Generated: ${new Date(report.generatedAt).toLocaleString('en-GB')}`);addHeading(filterLabel(report));
    if(report.member)addHeading(Object.values(report.member).filter(Boolean).join(' | '));
    addHeading(section.title,true);
    const header=sheet.addRow(columns.map(c=>c.label));
    header.eachCell(cell=>{cell.fill={type:'pattern',pattern:'solid',fgColor:{argb:'FF0F766E'}};cell.font={bold:true,color:{argb:'FFFFFFFF'}};cell.alignment={wrapText:true};});header.height=30;
    section.rows.forEach((item,rowIndex)=>{
      const row=sheet.addRow(columns.map(c=>c.type==='money'?item[c.key]??'Unavailable':c.type==='datetime'?cellText(item,c):item[c.key]??''));
      row.eachCell((cell,columnIndex)=>{cell.alignment={vertical:'top',wrapText:true};if(columns[columnIndex-1].type==='money'||index===0&&columnIndex===2&&item.money)cell.numFmt='#,##0.00;[Red](#,##0.00)';if(rowIndex%2===1)cell.fill={type:'pattern',pattern:'solid',fgColor:{argb:'FFF1F5F9'}};});
      row.height=30;
    });
    if(section.rows.length)sheet.autoFilter={from:{row:header.number,column:1},to:{row:header.number+section.rows.length,column:columns.length}};
    else addHeading('No records match the selected filters.');
    sheet.views=[{state:'frozen',ySplit:header.number}];
    columns.forEach((c,i)=>{sheet.getColumn(i+1).width=c.type==='money'?22:['description','memberName'].includes(c.key)?38:25;});
    for(const total of section.totals)addHeading(summaryText(total));
    if(index===0) {addHeading('Report notes',true);report.notes.forEach(note=>addHeading(note));}
    sheet.pageSetup.printTitlesRow=`1:${header.number}`;
  });
  return workbook;
}

export async function exportReport(report, format) {
  if(format==='pdf'){(await createReportPdf(report)).save(`${fileName(report)}.pdf`);return;}
  const workbook=await createReportWorkbook(report);
  const bytes=await workbook.xlsx.writeBuffer();
  const url=URL.createObjectURL(new Blob([bytes],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
  const anchor=document.createElement('a');anchor.href=url;anchor.download=`${fileName(report)}.xlsx`;anchor.click();setTimeout(()=>URL.revokeObjectURL(url),30000);
}

// Print the same paginated PDF as Export PDF, including all rows and page numbers.
export async function printReport(report) {
  const preview=window.open('about:blank','_blank');
  if(!preview)throw new Error('Allow pop-ups to open the printable report.');
  preview.document.title='Preparing printable report';
  preview.document.body.textContent='Preparing report…';
  try {
    const doc=await createReportPdf(report);doc.autoPrint();
    const url=URL.createObjectURL(doc.output('blob'));preview.location.replace(url);
    setTimeout(()=>URL.revokeObjectURL(url),120000);
  }catch(error){preview.close();throw error;}
}
