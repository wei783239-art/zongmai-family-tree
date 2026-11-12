const isParent = (edge) =>
  ["PARENT_CHILD", "ADOPTIVE_PARENT_CHILD"].includes(edge.relation_type);

export function traceIds(relations, start, direction = "descendants") {
  const next = new Map();
  for (const edge of relations.filter(isParent)) {
    const from = String(
      direction === "ancestors" ? edge.to_person_id : edge.from_person_id,
    );
    const to = String(
      direction === "ancestors" ? edge.from_person_id : edge.to_person_id,
    );
    if (!next.has(from)) next.set(from, []);
    next.get(from).push(to);
  }
  const visited = new Set([String(start)]);
  const queue = [String(start)];
  for (let i = 0; i < queue.length; i++) {
    for (const id of next.get(queue[i]) || [])
      if (!visited.has(id)) {
        visited.add(id);
        queue.push(id);
      }
  }
  visited.delete(String(start));
  return visited;
}

export function buildTreeData(people, relations) {
  const ids = new Set(people.map((p) => String(p.id)));
  const valid = relations.filter(
    (r) =>
      r.status === "CONFIRMED" &&
      ids.has(String(r.from_person_id)) &&
      ids.has(String(r.to_person_id)),
  );
  const buildGroups = (groupSpouses) => {
    const representative = new Map([...ids].map((id) => [id, id]));
    const find = (id) => {
      while (representative.get(id) !== id) id = representative.get(id);
      return id;
    };
    if (groupSpouses)
      for (const edge of valid.filter((r) => r.relation_type === "SPOUSE")) {
        representative.set(
          find(String(edge.to_person_id)),
          find(String(edge.from_person_id)),
        );
      }
    const groups = new Map();
    for (const person of people) {
      const id = find(String(person.id));
      if (!groups.has(id))
        groups.set(id, {
          id,
          people: [],
          children: new Set(),
          parents: new Set(),
          level: 0,
        });
      groups.get(id).people.push(person);
    }
    for (const edge of valid.filter(isParent)) {
      const from = find(String(edge.from_person_id)),
        to = find(String(edge.to_person_id));
      if (from === to) return null;
      groups.get(from).children.add(to);
      groups.get(to).parents.add(from);
    }
    const indegree = new Map(
      [...groups].map(([id, group]) => [id, group.parents.size]),
    );
    const queue = [...groups.values()].filter(
      (group) => group.parents.size === 0,
    );
    for (let i = 0; i < queue.length; i++)
      for (const childId of queue[i].children) {
        const child = groups.get(childId);
        child.level = Math.max(child.level, queue[i].level + 1);
        indegree.set(childId, indegree.get(childId) - 1);
        if (indegree.get(childId) === 0) queue.push(child);
      }
    return queue.length === groups.size ? queue : null;
  };
  // 跨代婚姻可能使配偶合并后的布局成环，此时使用独立人物的亲子层级。
  const groups = buildGroups(true) || buildGroups(false);
  if (!groups) throw new Error("亲子关系存在环路，请检查关系数据");
  const layers = new Map();
  for (const group of groups) {
    if (!layers.has(group.level)) layers.set(group.level, []);
    layers.get(group.level).push(group);
  }
  const nodes = [];
  for (const [level, row] of layers) {
    let x =
      -row.reduce((sum, group) => sum + group.people.length * 200 + 50, 0) / 2;
    for (const group of row) {
      for (const person of group.people) {
        nodes.push({
          id: String(person.id),
          data: person,
          style: { x: x + 100, y: level * 155 },
        });
        x += 200;
      }
      x += 50;
    }
  }
  const edges = valid.map((relation) => {
    const spouse = relation.relation_type === "SPOUSE";
    return {
      id: `r-${relation.id}`,
      source: String(relation.from_person_id),
      target: String(relation.to_person_id),
      type: spouse ? "line" : "polyline",
      data: relation,
      style: {
        stroke: spouse ? "#a97d43" : "#2f6156",
        lineWidth: 1.7,
        lineDash: spouse
          ? [6, 4]
          : relation.relation_type === "ADOPTIVE_PARENT_CHILD"
            ? [3, 3]
            : [],
        endArrow: !spouse,
      },
    };
  });
  return { nodes, edges };
}
