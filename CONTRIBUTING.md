# Contributing to AMAN / DMAN

Thanks for your interest in contributing!  
This project aims to model realistic arrival and departure management logic for use on VATSIM and similar simulation networks.

Contributions are very welcome — especially improvements to trajectory prediction, sequencing logic, performance models, and controller-facing behavior.

---

## Philosophy

This project works best when **behavioral changes are shared upstream**.

If you fork the repository to experiment, that’s absolutely fine.  
If your changes affect **logic, algorithms, or operational behavior**, please consider opening a Pull Request so the community can converge on a common, realistic implementation.

This is especially important for:
- AMAN / DMAN sequencing rules
- TTL / TTG logic
- Aircraft performance modeling
- Trajectory prediction
- Separation and spacing behavior

---

## What makes a good Pull Request

A PR does not need to be perfect. Small, focused improvements are encouraged.

Good PRs typically:
- Change one logical thing
- Explain *why* the behavior was changed
- Reference real-world procedures or documents when relevant
- Avoid large refactors unless discussed first

If you are unsure whether something belongs upstream — open a draft PR or start a discussion.

---

## Coding guidelines

- Keep logic deterministic where possible
- Avoid UI-specific assumptions in domain logic
- Prefer clarity over cleverness
- Comments explaining *operational intent* are highly valued

Realism > elegance.

---

## Licensing

This project is licensed under **GPL-3.0**.

If you distribute a modified version, you must make the source code available under the same license.  
Private experimentation and local forks are, of course, allowed.

---

## Questions & discussion

If you want to:
- Propose a different AMAN concept
- Change sequencing or control philosophy
- Align behavior with real-world operations

Please open an issue or discussion before implementing large changes.

We’re happy to discuss ideas early 🙂
