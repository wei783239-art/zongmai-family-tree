import test from 'node:test'
import assert from 'node:assert/strict'
import { buildTreeData, traceIds } from '../frontend/src/utils/treeLayout.js'
const person = id => ({ id, name:`Person ${id}`, generation:99 })
const edge = (id, from, to, type = 'PARENT_CHILD') => ({ id, from_person_id:from, to_person_id:to, relation_type:type, status:'CONFIRMED' })

test('two parents, spouse, multiple generations, disconnected person', () => {
  const data = buildTreeData([1,2,3,4,5].map(person),[edge(1,1,2,'SPOUSE'),edge(2,1,3),edge(3,2,3),edge(4,3,4)])
  const y = id => data.nodes.find(n => n.id === String(id)).style.y
  assert.equal(y(1),y(2)); assert.ok(y(3)>y(1)); assert.ok(y(4)>y(3))
  assert.equal(data.nodes.length,5)
  assert.equal(new Set(data.nodes.map(n => `${n.style.x},${n.style.y}`)).size,5)
})
test('trace de-duplicates shared ancestors and excludes spouse traversal', () => {
  const edges = [edge(1,1,3),edge(2,2,3),edge(3,3,4),edge(4,1,2,'SPOUSE')]
  assert.deepEqual([...traceIds(edges,'4','ancestors')].sort(),['1','2','3'])
  assert.deepEqual([...traceIds(edges,'1')].sort(),['3','4'])
})
test('cross-generation spouse does not collapse parent levels', () => {
  const data = buildTreeData([1,2,3].map(person),[edge(1,1,2),edge(2,2,3),edge(3,1,3,'SPOUSE')])
  assert.ok(data.nodes.find(n=>n.id==='3').style.y > data.nodes.find(n=>n.id==='1').style.y)
})
test('cycle is rejected and invisible endpoints are removed', () => {
  assert.throws(() => buildTreeData([1,2].map(person),[edge(1,1,2),edge(2,2,1)]),/环路/)
  assert.equal(buildTreeData([person(1)],[edge(1,1,999)]).edges.length,0)
})
