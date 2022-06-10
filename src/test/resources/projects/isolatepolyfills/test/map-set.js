/**
 * @template T
 * @param {T[]} items 
 * @return {T[]}
 */
function makeUnique(items) {
	const set = new Set(items);
	return Array.from(set);
}
/**
 * @template T
 * @param {T[]} items 
 * @param {(item: T) => any} keyExtractor 
 */
function uniqueByKey(items, keyExtractor) {
	const map = new Map(items.map(item => [keyExtractor(item), item]));
	return Array.from(map.values());
}