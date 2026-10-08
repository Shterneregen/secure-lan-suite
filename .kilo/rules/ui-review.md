# UI Review Rule

Use `.kilo/checklists/review.md` for every completed UI review.

Every UI review must produce:

1. Findings ordered by severity.
2. User impact and concrete evidence.
3. Violated Product UI Guide constraints.
4. Concrete fixes.
5. Final decision: Accept / Needs Refinement / Reject.

Require revision if the change:

- breaks required behavior or loses user context;
- makes communication hard to find or use;
- exposes technical details without helping a decision or recovery;
- introduces inaccessible controls or unusable responsive states;
- recreates the deprecated JavaFX layout or an administration dashboard.
