import service from '@/utils/request'

export function listFamilies() {
  return service.get('/families')
}

export function listPersonsByFamily(familyId, name = '') {
  return service.get(`/families/${familyId}/persons`, {
    params: { name }
  })
}

export function createPerson(familyId, person) {
  return service.post(`/families/${familyId}/persons`, person)
}

export function updatePerson(familyId, personId, person) {
  return service.put(`/families/${familyId}/persons/${personId}`, person)
}

export function deletePerson(familyId, personId) {
  return service.delete(`/families/${familyId}/persons/${personId}`)
}

export function listRelationsByFamily(familyId) {
  return service.get(`/families/${familyId}/relations`)
}

export function createRelation(familyId, relation) {
  return service.post(`/families/${familyId}/relations`, relation)
}

export function deleteRelation(familyId, relationId) {
  return service.delete(`/families/${familyId}/relations/${relationId}`)
}

export function getPerson(familyId, personId) {
  return service.get(`/families/${familyId}/persons/${personId}`)
}

export function createFamily(family) {
  return service.post(`/families`, family)
}

export function updateFamliy(familyId, family) {
  return service.put(`/families/${familyId}`, family)
}
 
export function deleteFamily(familyId) {
  return service.delete(`/families/${familyId}`)
}

export function pagePersonsByFamily(familyId, page = 1, size = 10, name = '') {
  return service.get(`/families/${familyId}/persons/page`,{ params: { page, size, name }})
}

export function getFamily(familyId) {
  return service.get(`/families/${familyId}`)
}

export function createSpouseRelation(familyId, data) {
  return service.post(`/families/${familyId}/relations/spouse`,data)
}

export function listAncestors(familyId, personId) {
  return service.get(`/families/${familyId}/persons/${personId}/ancestors`)
}

export function listDescendants(familyId, personId) {
  return service.get(`/families/${familyId}/persons/${personId}/descendants`)
}

