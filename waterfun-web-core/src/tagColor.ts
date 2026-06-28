const TAG_COLORS = [
  '#409EFF', '#60A5FA', '#818CF8', '#A78BFA',
  '#34D399', '#2DD4BF', '#38BDF8', '#22D3EE',
  '#67C23A', '#6EE7B7', '#F56C6C', '#FB7185',
  '#F2A7B3', '#E879F9', '#909399', '#9CA3AF',
]

function hash(name: string): number {
  let h = 0
  for (let i = 0; i < name.length; i++) {
    h = ((h << 5) - h) + name.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

export function getTagColor(tagName: string): string {
  if (!tagName) return TAG_COLORS[0]!
  return TAG_COLORS[hash(tagName.toLowerCase()) % TAG_COLORS.length] ?? TAG_COLORS[0]!
}
