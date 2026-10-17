# Defensive fixture labels

`defensive-fixtures.csv` contains 20 isolated, structurally consistent positions: five each for Lamb/Tiger in placement/movement. These are tactical fixtures, not claimed opening-reachable histories and never valid Continue imports.

The fixed `safeActions` sets were enumerated independently in Python from D017's ten board lines (seed 17017). Each listed action avoids an immediate opponent win; every other legal action permits one. The test verifies the whole expected set against every immediate reply before asking Expert to choose. These labels are a bounded two-ply oracle, not a claim of a forced eventual win. Tags are P(destination), S(origin,destination), C(origin,middle,destination).

The fixtures supplement 20 immediate fifth-capture positions and placement containment cases. They exposed equal-valued forced-loss lines in search: terminal scoring now prefers earlier wins and delays unavoidable losses, so an immediate loss does not tie with continued resistance.
