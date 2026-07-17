# Design QA — Signal & Flow 0.7.0

## Compared

- Source: combined black/electric-green Signal & Flow mobile concept.
- Implementation: Pixel 10a capture at 1080 × 2424, Simple and Wireless Engineer states.
- Evidence: `qa/06-test-simple-final.png`, `qa/07-engineer-top.png`, and `qa/08-engineer-evidence.png`.

## Result

The implementation preserves the selected concept's core hierarchy: true-black field UI, high-contrast green primary action, two-level mode switch, plain-language verdict, three outcome rows, expandable evidence, and five outcome-based navigation destinations. The live implementation intentionally replaces unsupported concept measurements such as phone-derived noise floor and spectrum utilization with values Android can substantiate: scan provenance, observed AP sample count, RSSI distribution, congestion count, and active performance-test status.

## Findings resolved

- P1: The first implementation used the existing teal navigation accent. Updated selected navigation states to the same electric green as the primary workflow.
- P1: Engineer content could exceed the usable Pixel viewport. Added vertical scrolling so evidence and Start Investigation remain reachable.
- P2: A duplicate app title consumed vertical space. Removed the redundant top bar and retained the branded in-screen header.
- P2: The client walk test was only an implied destination in the concept. Added a functional Survey state with explicit phone-only limitations and an active/capture interaction.

## Remaining polish

- P3: A future iteration can add a generated, branded RF mark and richer motion without changing the measurement model.
- P3: Connection details depend on Android permission and current Wi-Fi state; the disconnected state is intentionally explicit.

## Verification

- Unit tests: passed.
- Android lint: passed.
- Debug APK assembly: passed.
- Installed and launched on connected Pixel: passed.
- Simple/Engineer mode switch: passed.
- Engineer evidence expansion and scrolling: passed.
- Survey navigation and active/capture state: passed.

final result: passed
