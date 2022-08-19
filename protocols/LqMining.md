# ErgoDEX liquidity mining protocol

Liqudity Mining Protocol allows anyone to setup his own liquidity mining (LM) program targeted at the desired pool on ErgoDEX.

## Liquidity Mining Pool
Liquidity Mining (LM) Pool is represented on-chain as a UTxO with the following structure:

### Datum (LM configuration)
| Field           | Type      | Description                    |
|-----------------|-----------|--------------------------------|
| `frameLen`      | `Integer` | Length of one frame in blocks  |
| `epochLen`      | `Integer` | Length of one epoch in frames  |
| `epochNum`      | `Integer` | Number of epochs in LM program |
| `programStart`  | `Integer` | Block the program starts at    |
| `programBudget` | `Integer` | Total LM program budget        |

### Tokens
| Name           | Description                                                       |
|----------------|-------------------------------------------------------------------|
| Reward token   | Budget of the LM program                                          |
| LQ token       | Locked LQ tokens                                                  |
| vLQ token      | Tokens representing locked share of LQ                            |
| Temporal Token | Tokens representing number of frames until the end of the program |

## Staking bundle
Staking bundle is responsible for holding vLQ and Epoch tokens. Staking bundle script guarantees bundling of tokens and controls Compounding and Redeem operations (see "User scenarios" below).

## User scenarios

### Create LM Pool
Alice works on a project X with a token Xt. She would like to incentivize holders of Xt to keep their tokens in ADA/Xt pools.
To do that, Alice sets parameters `epochLen` and `epochNum` and sends `U` tokens Xt to the LM script address, where `B_t` - total budget of LM program.

### Deposit
Bob wants to participate in LM program X. To do that, he sends `N` ADA/Xt LQ tokens to LM script address and receives bundled (see "Staking bundle" section above) `M` vLQ tokens + `K` Epoch tokens in return, where `N` - amount of LQ tokens deposited, `M = N`, `K = EpochEnd - Epoch`

### Compounding
Compounding must be performed each epoch so that rewards allocated for each epoch are fully distributed among stakers. Each staker receives `EpochReward * M / Emission_M` tokens. `1` Epoch token is withdrawn from staker's staking bundle box each time compounding happens.

### Redeem
Once Bob decided to unstake his liquidity he returns his staking bundle to LM Pool and receives proportional amount of LQ tokens to the amount of vLQ returned. Redemption is only allowed when all epochs Bob is eligible for are compounded.

_Properties_:
* User can redeem his LQ tokens before and after program end

### Increase stake
Bob wants to stake more LQ tokens. Once he deposited additional liqudity to LM Pool the released vLQ are addded to the Bob's staking bundle. No additional Epoch tokens are released. 
